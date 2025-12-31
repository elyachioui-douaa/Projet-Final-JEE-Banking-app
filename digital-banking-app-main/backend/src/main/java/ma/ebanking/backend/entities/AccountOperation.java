package ma.ebanking.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.ebanking.backend.enums.OperationType;

import java.util.Date;

/**
 * Entité représentant une opération bancaire effectuée sur un compte
 * Enregistre toutes les transactions (dépôts, retraits, virements, etc.)
 */
@Entity // Indique que cette classe est une entité JPA mappée à une table en base de données
@Data // Annotation Lombok : génère automatiquement getters, setters, toString, equals et hashCode
@NoArgsConstructor // Génère un constructeur vide (requis par JPA)
@AllArgsConstructor // Génère un constructeur avec tous les paramètres
public class AccountOperation {

    /**
     * Identifiant unique de l'opération bancaire
     * Généré automatiquement par la base de données
     */
    @Id // Définit ce champ comme clé primaire de la table
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incrémentation automatique de l'identifiant
    private Long id;

    /**
     * Date et heure de l'exécution de l'opération
     * Permet de tracer l'historique chronologique des transactions
     */
    private Date operationDate;

    /**
     * Montant de l'opération en devise
     * Peut être positif (crédit/dépôt) ou négatif (débit/retrait)
     */
    private double amount;

    /**
     * Type d'opération effectuée (DEBIT, CREDIT, TRANSFER, etc.)
     * Stocké en base comme chaîne de caractères pour une meilleure lisibilité
     */
    @Enumerated(EnumType.STRING) // Stocke le nom de l'enum plutôt que sa valeur ordinale (0, 1, 2...)
    private OperationType type;

    /**
     * Référence au compte bancaire concerné par cette opération
     * Relation Many-to-One : plusieurs opérations appartiennent à un seul compte
     */
    @ManyToOne // Crée une clé étrangère vers la table BankAccount
    private BankAccount bankAccount;

    /**
     * Description textuelle de l'opération
     * Exemple : "Retrait DAB", "Virement salaire", "Achat en ligne"
     */
    private String description;
}