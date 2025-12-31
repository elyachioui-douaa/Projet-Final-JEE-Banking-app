package ma.ebanking.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.ebanking.backend.enums.AccountStatus;

import java.util.Date;
import java.util.List;

/**
 * Classe abstraite représentant un compte bancaire générique
 * Cette classe sert de base pour différents types de comptes (compte courant, compte épargne, etc.)
 */
@Entity // Indique que cette classe est une entité JPA
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // Stratégie d'héritage : toutes les sous-classes sont stockées dans une seule table
@DiscriminatorColumn(name = "TYPE", length = 4) // Colonne discriminante pour différencier les types de comptes dans la table
@Data // Génère automatiquement les getters, setters, toString, equals et hashCode
@NoArgsConstructor // Génère un constructeur sans paramètres
@AllArgsConstructor // Génère un constructeur avec tous les paramètres
public abstract class BankAccount {

    /**
     * Identifiant unique du compte bancaire
     * Non auto-généré, doit être défini manuellement (probablement un numéro de compte)
     */
    @Id // Définit ce champ comme clé primaire
    private String id;
    /**
     * Solde actuel du compte
     */
    private double balance;
    /**
     * Date de création du compte
     */
    private Date createdAt;
    /**
     * Statut du compte (ACTIF, SUSPENDU, BLOQUÉ, etc.)
     * Stocké sous forme de chaîne de caractères dans la base de données
     */
    @Enumerated(EnumType.STRING) // Stocke l'énumération en tant que String plutôt qu'en ordinal
    private AccountStatus status;
    /**
     * Client propriétaire de ce compte
     * Relation Many-to-One : plusieurs comptes peuvent appartenir à un même client
     */
    @ManyToOne // Relation bidirectionnelle avec Customer
    private Customer customer;

    /**
     * Liste des opérations effectuées sur ce compte
     * Relation One-to-Many : un compte peut avoir plusieurs opérations
     */
    @OneToMany(mappedBy = "bankAccount", fetch = FetchType.LAZY) // Chargement paresseux : les opérations ne sont chargées que si nécessaire
    private List<AccountOperation> accountOperations;
}