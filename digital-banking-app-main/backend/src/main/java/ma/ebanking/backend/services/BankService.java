package ma.ebanking.backend.services;

import ma.ebanking.backend.entities.BankAccount;
import ma.ebanking.backend.entities.CurrentAccount;
import ma.ebanking.backend.entities.SavingAccount;
import ma.ebanking.backend.repositories.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service utilitaire pour consulter et afficher les informations détaillées d'un compte bancaire
 * Principalement utilisé pour le débogage et les tests
 */
@Service // Marque cette classe comme un composant de service Spring
@Transactional // Toutes les méthodes s'exécutent dans une transaction
public class BankService {

    /**
     * Repository pour accéder aux comptes bancaires en base de données
     * Injection de dépendance via @Autowired
     */
    @Autowired
    private BankAccountRepository bankAccountRepository;

    /**
     * Méthode de consultation détaillée d'un compte bancaire
     * Affiche toutes les informations du compte et ses opérations dans la console
     *
     * Cette méthode est utile pour :
     * - Le débogage pendant le développement
     * - La vérification des données en base
     * - Les tests manuels
     *
     * Note : Dans un environnement de production, il serait préférable d'utiliser
     * un logger (SLF4J) plutôt que System.out.println
     */
    public void consulter() {
        // Recherche d'un compte spécifique par son identifiant UUID
        // L'ID est codé en dur ici (probablement pour des tests)
        BankAccount bankAccount =
                bankAccountRepository.findById("0b36be78-8d5d-446b-9f20-37eadc9d3c3b").orElse(null);

        // Vérification que le compte existe
        if (bankAccount != null) {
            System.out.println("*****************************");

            // Affichage des informations générales du compte
            System.out.println(bankAccount.getId()); // Identifiant unique du compte
            System.out.println(bankAccount.getBalance()); // Solde actuel
            System.out.println(bankAccount.getStatus()); // Statut (ACTIF, SUSPENDU, etc.)
            System.out.println(bankAccount.getCreatedAt()); // Date de création
            System.out.println(bankAccount.getCustomer().getName()); // Nom du client propriétaire
            System.out.println(bankAccount.getClass().getSimpleName()); // Type de compte (CurrentAccount ou SavingAccount)

            /**
             * Gestion du polymorphisme : affichage des attributs spécifiques selon le type de compte
             *
             * - CurrentAccount : possède un découvert autorisé (overDraft)
             * - SavingAccount : possède un taux d'intérêt (interestRate)
             */
            if (bankAccount instanceof CurrentAccount) {
                // Cast vers CurrentAccount pour accéder à l'attribut overDraft
                System.out.println("Over Draft=>" + ((CurrentAccount) bankAccount).getOverDraft());
            } else if (bankAccount instanceof SavingAccount) {
                // Cast vers SavingAccount pour accéder à l'attribut interestRate
                System.out.println("Rate=>" + ((SavingAccount) bankAccount).getInterestRate());
            }

            /**
             * Affichage de l'historique des opérations du compte
             * Utilisation de forEach avec une expression lambda pour parcourir la liste
             *
             * Pour chaque opération, affiche :
             * - Le type (DEBIT, CREDIT, TRANSFER)
             * - La date de l'opération
             * - Le montant
             */
            bankAccount.getAccountOperations().forEach(op -> {
                System.out.println(op.getType() + "\t" + op.getOperationDate() + "\t" + op.getAmount());
            });
        }
    }
}