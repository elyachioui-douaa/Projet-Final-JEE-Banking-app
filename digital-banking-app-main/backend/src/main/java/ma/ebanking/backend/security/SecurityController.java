package ma.ebanking.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour la gestion de l'authentification et de l'autorisation
 * Fournit les endpoints pour :
 * - La connexion des utilisateurs (génération de tokens JWT)
 * - La récupération du profil de l'utilisateur authentifié
 */
@RestController // Combinaison de @Controller et @ResponseBody - toutes les méthodes retournent du JSON
@RequestMapping("/auth") // Préfixe de base pour tous les endpoints de ce contrôleur
public class SecurityController {

    /**
     * Gestionnaire d'authentification Spring Security
     * Injecté automatiquement par Spring (configuré dans SecurityConfig)
     * Responsable de valider les credentials (username/password) fournis par l'utilisateur
     */
    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Encodeur JWT pour créer des tokens sécurisés
     * Injecté automatiquement (bean défini dans SecurityConfig)
     * Utilise la clé secrète et l'algorithme HMAC-SHA512 pour signer les tokens
     */
    @Autowired
    private JwtEncoder jwtEncoder;

    /**
     * Endpoint pour récupérer le profil de l'utilisateur actuellement authentifié
     * Retourne les informations de l'utilisateur extrait du token JWT
     *
     * URL : GET /auth/profile
     * Headers requis : Authorization: Bearer <jwt_token>
     *
     * @param authentication Objet d'authentification injecté automatiquement par Spring Security
     *                       Contient les informations de l'utilisateur connecté :
     *                       - Principal (nom d'utilisateur)
     *                       - Authorities (rôles et permissions)
     *                       - Credentials (généralement null après authentification)
     *                       - Authentifié (true/false)
     * @return L'objet Authentication sérialisé en JSON avec :
     *         - name : nom d'utilisateur
     *         - authorities : liste des rôles
     *         - authenticated : statut d'authentification
     *
     * Exemple de réponse JSON :
     * {
     *   "authorities": [{"authority": "USER"}, {"authority": "ADMIN"}],
     *   "details": null,
     *   "authenticated": true,
     *   "principal": {
     *     "username": "admin",
     *     ...
     *   },
     *   "name": "admin"
     * }
     */
    @GetMapping("/profile")
    public Authentication authetication(Authentication authentication) {
        // Spring Security injecte automatiquement l'objet Authentication
        // en décodant et validant le token JWT présent dans l'en-tête Authorization
        return authentication;
    }

    /**
     * Endpoint de connexion - Authentifie l'utilisateur et génère un token JWT
     * C'est le point d'entrée principal pour l'authentification dans l'application
     *
     * URL : POST /auth/login
     * Content-Type : application/x-www-form-urlencoded ou multipart/form-data
     * Body : username=xxx&password=yyy
     *
     * @param username Nom d'utilisateur fourni par le client (ex: "admin", "user1")
     * @param password Mot de passe en clair (sera vérifié contre le hash BCrypt stocké)
     * @return Map contenant le token JWT sous la clé "access-token"
     *
     * Exemple de réponse JSON :
     * {
     *   "access-token": "eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJzZWxmIiwic3ViIjoidXNlcjEi..."
     * }
     *
     * @throws org.springframework.security.core.AuthenticationException
     *         Si les credentials sont invalides (mauvais username ou password)
     *         Spring Security retournera automatiquement un HTTP 401 Unauthorized
     *
     * Processus complet :
     * 1. Validation des credentials via AuthenticationManager
     * 2. Extraction des rôles/permissions de l'utilisateur
     * 3. Construction des claims du JWT (données embarquées dans le token)
     * 4. Signature du token avec la clé secrète
     * 5. Retour du token au client
     */
    @PostMapping("/login")
    public Map<String, String> login(String username, String password) {
        /**
         * ÉTAPE 1 : Authentification
         * Création d'un token d'authentification temporaire avec les credentials
         * AuthenticationManager délègue la vérification au DaoAuthenticationProvider
         * qui compare le password avec le hash BCrypt stocké
         */
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        // Si l'authentification échoue, une exception est levée et le code suivant n'est pas exécuté

        /**
         * ÉTAPE 2 : Préparation des métadonnées temporelles du token
         */
        Instant instant = Instant.now(); // Horodatage actuel (précis à la nanoseconde)

        /**
         * ÉTAPE 3 : Extraction et formatage des autorisations (rôles/permissions)
         * Les authorities sont des objets GrantedAuthority (ex: "USER", "ADMIN")
         * On les convertit en une chaîne de caractères séparée par des espaces
         * Format résultat : "USER ADMIN" ou simplement "USER"
         *
         * Cette information sera stockée dans le token et utilisée pour
         * vérifier les autorisations lors des requêtes ultérieures
         */
        String scope = authentication.getAuthorities()
                .stream() // Crée un flux à partir de la collection d'authorities
                .map(GrantedAuthority::getAuthority) // Extrait le nom de chaque authority
                .collect(Collectors.joining(" ")); // Concatène avec des espaces

        /**
         * ÉTAPE 4 : Construction du JWT Claims Set (payload du token)
         * Les claims sont les revendications/informations stockées dans le token
         * Ce sont des paires clé-valeur qui seront encodées et signées
         */
        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                .issuedAt(instant) // "iat" (Issued At) : quand le token a été créé
                .expiresAt(instant.plus(10, ChronoUnit.MINUTES)) // "exp" (Expiration) : validité de 10 minutes
                .subject(username) // "sub" (Subject) : identifiant de l'utilisateur
                .claim("scope", scope) // Claim personnalisé : les rôles de l'utilisateur
                .build();

        // Note : On pourrait ajouter d'autres claims personnalisés :
        // .claim("email", user.getEmail())
        // .claim("userId", user.getId())

        /**
         * ÉTAPE 5 : Configuration des paramètres d'encodage du JWT
         * Spécifie :
         * - L'en-tête JWS (JSON Web Signature) avec l'algorithme de signature
         * - Les claims à encoder dans le payload
         */
        JwtEncoderParameters jwtEncoderParameters =
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS512).build(), // Header : algorithme HMAC-SHA512
                        jwtClaimsSet // Payload : les claims définis ci-dessus
                );

        /**
         * ÉTAPE 6 : Encodage et signature du JWT
         * Le jwtEncoder utilise la clé secrète (définie dans SecurityConfig)
         * pour créer une signature cryptographique qui garantit :
         * - L'intégrité : le token n'a pas été modifié
         * - L'authenticité : le token provient bien de notre serveur
         *
         * Structure du JWT généré :
         * HEADER.PAYLOAD.SIGNATURE
         *
         * Exemple :
         * eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJzZWxmIiwic3ViIjoidXNlcjEiLCJleHAiOjE2ODk...
         * └─────header─────┘ └───────────────payload──────────────┘ └──signature──┘
         */
        String jwt = jwtEncoder.encode(jwtEncoderParameters).getTokenValue();

        /**
         * ÉTAPE 7 : Retour du token au client
         * Le client doit :
         * 1. Stocker ce token (localStorage, sessionStorage, cookie httpOnly)
         * 2. L'inclure dans toutes les requêtes ultérieures via l'en-tête :
         *    Authorization: Bearer <jwt_token>
         *
         * Exemple d'utilisation côté client (JavaScript) :
         * fetch('/api/customers', {
         *   headers: {
         *     'Authorization': 'Bearer ' + token
         *   }
         * })
         */
        return Map.of("access-token", jwt);
    }
}