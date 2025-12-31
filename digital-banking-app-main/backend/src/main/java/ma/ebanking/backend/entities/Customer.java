package ma.ebanking.backend.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Entité représentant un client de la banque
 * Cette classe est mappée à une table dans la base de données
 */
@Entity // Indique que cette classe est une entité JPA
@Data // Génère automatiquement les getters, setters, toString, equals et hashCode
@NoArgsConstructor // Génère un constructeur sans paramètres
@AllArgsConstructor // Génère un constructeur avec tous les paramètres
public class Customer {

    /**
     * Identifiant unique du client
     * Généré automatiquement par la base de données
     */
    @Id // Définit ce champ comme clé primaire
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incrémentation de l'ID
    private Long id;
    private String name;
    private String email;
    /**
     * Liste des comptes bancaires associés à ce client
     * Relation One-to-Many : un client peut avoir plusieurs comptes
     */
    @OneToMany(mappedBy = "customer") // Relation bidirectionnelle, "customer" est l'attribut dans BankAccount
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // Empêche la sérialisation de cette liste lors de la conversion en JSON (évite les boucles infinies)
    private List<BankAccount> bankAccounts;
}