package ma.ebanking.backend.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;

/**
 * Configuration principale de la sécurité de l'application
 * Configure l'authentification JWT, les autorisations, CORS et les utilisateurs
 */
@Configuration // Indique que cette classe contient des beans de configuration Spring
@EnableWebSecurity // Active la configuration de sécurité Web Spring Security
@EnableMethodSecurity(prePostEnabled = true) // Active la sécurité au niveau des méthodes (@PreAuthorize, @PostAuthorize)
public class SecurityConfig {

    /**
     * Clé secrète pour signer les tokens JWT
     * Chargée depuis le fichier application.properties/yml via ${jwt.secret}
     * Cette clé doit être gardée confidentielle et complexe en production
     */
    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Gestionnaire d'utilisateurs en mémoire (pour le développement/tests)
     * En production, on utiliserait plutôt une base de données
     *
     * @return InMemoryUserDetailsManager contenant les utilisateurs de test
     */
    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        PasswordEncoder passwordEncoder = passwordEncoder();
        return new InMemoryUserDetailsManager(
                // Utilisateur standard avec le rôle USER
                User.withUsername("user1")
                        .password(passwordEncoder.encode("12345")) // Mot de passe encodé avec BCrypt
                        .authorities("USER") // Rôle/autorisation
                        .build(),

                // Administrateur avec les rôles USER et ADMIN
                User.withUsername("admin")
                        .password(passwordEncoder.encode("12345"))
                        .authorities("USER", "ADMIN") // Plusieurs rôles possibles
                        .build()
        );
    }

    /**
     * Encodeur de mots de passe utilisant l'algorithme BCrypt
     * BCrypt est recommandé pour sa sécurité (salage automatique, résistant au brute force)
     *
     * @return Instance de BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Chaîne de filtres de sécurité - cœur de la configuration Spring Security
     * Définit les règles d'autorisation, la gestion de session, CORS, etc.
     *
     * @param httpSecurity Objet de configuration HTTP Security
     * @return SecurityFilterChain configurée
     * @throws Exception En cas d'erreur de configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                /**
                 * Configuration de la gestion de session : STATELESS
                 * Aucune session HTTP n'est créée (adapté pour les API REST avec JWT)
                 * Chaque requête doit contenir le token JWT pour être authentifiée
                 */
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                /**
                 * Désactivation de la protection CSRF (Cross-Site Request Forgery)
                 * Non nécessaire pour les API REST stateless avec JWT
                 * CSRF est pertinent uniquement pour les applications avec session/cookies
                 */
                .csrf(csrf -> csrf.disable())

                /**
                 * Activation de CORS (Cross-Origin Resource Sharing)
                 * Permet aux applications frontend (ex: Angular, React) hébergées sur d'autres domaines
                 * d'accéder à cette API
                 */
                .cors(Customizer.withDefaults())

                /**
                 * Configuration des autorisations HTTP
                 * Définit quels endpoints sont publics et lesquels nécessitent une authentification
                 */
                .authorizeHttpRequests(ar -> ar.requestMatchers("/auth/login/**").permitAll()) // Endpoint de connexion accessible sans authentification
                .authorizeHttpRequests(ar -> ar.anyRequest().authenticated()) // Toutes les autres requêtes nécessitent une authentification

                /**
                 * Configuration du serveur de ressources OAuth2 avec JWT
                 * Spring Security validera automatiquement les tokens JWT sur toutes les requêtes authentifiées
                 */
                .oauth2ResourceServer(oa -> oa.jwt(Customizer.withDefaults()))

                .build();
    }

    /**
     * Encodeur JWT pour créer et signer les tokens
     * Utilise la clé secrète pour signer les tokens avec l'algorithme HMAC
     *
     * @return JwtEncoder configuré avec la clé secrète
     */
    @Bean
    JwtEncoder jwtEncoder() {
        // ImmutableSecret crée une source de clé immuable à partir de la clé secrète
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey.getBytes()));
    }

    /**
     * Décodeur JWT pour valider et décoder les tokens reçus
     * Vérifie la signature et extrait les claims (informations) du token
     *
     * @return JwtDecoder configuré avec la clé secrète et l'algorithme HS512
     *
     * Note : Il y a une petite incohérence ici - "RSA" devrait être remplacé par
     * le nom de l'algorithme HMAC, mais cela fonctionne car seul l'algorithme
     * spécifié dans macAlgorithm() est réellement utilisé
     */
    @Bean
    JwtDecoder jwtDecoder() {
        // Création d'une clé secrète pour HMAC (pas RSA malgré le nom du paramètre)
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "RSA");
        return NimbusJwtDecoder.withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512) // Algorithme de signature : HMAC-SHA512
                .build();
    }

    /**
     * Gestionnaire d'authentification
     * Responsable de vérifier les credentials (username/password) lors de la connexion
     *
     * @param userDetailsService Service fournissant les détails des utilisateurs
     * @return AuthenticationManager configuré
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService) {
        // Provider qui utilise une source de données (DAO) pour l'authentification
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        // Configure l'encodeur de mot de passe pour comparer les mots de passe hashés
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        // Retourne un gestionnaire qui délègue au provider
        return new ProviderManager(daoAuthenticationProvider);
    }

    /**
     * Configuration CORS (Cross-Origin Resource Sharing)
     * Définit les règles pour permettre aux applications frontend d'accéder à l'API
     *
     * @return CorsConfigurationSource avec les règles CORS
     *
     * ATTENTION : Cette configuration est très permissive (allowedOrigin: "*")
     * En production, il faudrait restreindre aux domaines spécifiques autorisés
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        /**
         * Autorise toutes les origines (domaines)
         * PRODUCTION : Remplacer par les domaines spécifiques, ex:
         * corsConfiguration.addAllowedOrigin("https://mon-frontend.com");
         */
        corsConfiguration.addAllowedOrigin("*");

        /**
         * Autorise toutes les méthodes HTTP (GET, POST, PUT, DELETE, etc.)
         */
        corsConfiguration.addAllowedMethod("*");

        /**
         * Autorise tous les en-têtes HTTP
         * Important pour Authorization (contient le token JWT)
         */
        corsConfiguration.addAllowedHeader("*");

        // Enregistre la configuration CORS pour tous les endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}