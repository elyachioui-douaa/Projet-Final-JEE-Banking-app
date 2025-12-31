package ma.ebanking.backend.services;

import ma.ebanking.backend.dto.*;
import ma.ebanking.backend.exceptions.BalanceNotSufficientException;
import ma.ebanking.backend.exceptions.BankAccountNotFoundException;
import ma.ebanking.backend.exceptions.CustomerNotFoundException;

import java.util.List;

/**
 * Interface de service définissant les opérations métier de la banque
 * Sert de contrat entre la couche contrôleur et la couche de logique métier
 * Utilise des DTOs (Data Transfer Objects) pour échanger des données avec la couche présentation
 */
public interface BankAccountService {

    /**
     * Enregistre un nouveau client dans le système
     *
     * @param customerDTO Objet contenant les informations du client à créer
     * @return CustomerDTO Le client créé avec son identifiant généré
     */
    CustomerDTO saveCustomer(CustomerDTO customerDTO);

    /**
     * Crée un compte courant avec découvert autorisé
     *
     * @param initialBalance Solde initial du compte
     * @param overDraft Montant du découvert autorisé (permet d'avoir un solde négatif)
     * @param customerId Identifiant du client propriétaire du compte
     * @return CurrentBankAccountDTO Le compte courant créé
     * @throws CustomerNotFoundException Si le client spécifié n'existe pas
     */
    CurrentBankAccountDTO saveCurrentBankAccount(double initialBalance, double overDraft, Long customerId) throws CustomerNotFoundException;

    /**
     * Crée un compte épargne avec taux d'intérêt
     *
     * @param initialBalance Solde initial du compte
     * @param interestRate Taux d'intérêt annuel appliqué (ex: 0.05 pour 5%)
     * @param customerId Identifiant du client propriétaire du compte
     * @return SavingBankAccountDTO Le compte épargne créé
     * @throws CustomerNotFoundException Si le client spécifié n'existe pas
     */
    SavingBankAccountDTO saveSavingBankAccount(double initialBalance, double interestRate, Long customerId) throws CustomerNotFoundException;

    /**
     * Récupère la liste de tous les clients
     *
     * @return Liste de tous les clients enregistrés dans le système
     */
    List<CustomerDTO> listCustomers();

    /**
     * Récupère les détails d'un compte bancaire spécifique
     *
     * @param accountId Identifiant unique du compte bancaire
     * @return BankAccountDTO Les informations du compte (peut être un compte courant ou épargne)
     * @throws BankAccountNotFoundException Si le compte n'existe pas
     */
    BankAccountDTO getBankAccount(String accountId) throws BankAccountNotFoundException;

    /**
     * Effectue un débit (retrait) sur un compte bancaire
     * Diminue le solde du compte du montant spécifié
     *
     * @param accountId Identifiant du compte à débiter
     * @param amount Montant à retirer
     * @param description Libellé de l'opération (ex: "Retrait DAB", "Paiement facture")
     * @throws BankAccountNotFoundException Si le compte n'existe pas
     * @throws BalanceNotSufficientException Si le solde est insuffisant pour effectuer le retrait
     */
    void debit(String accountId, double amount, String description) throws BankAccountNotFoundException, BalanceNotSufficientException;

    /**
     * Effectue un crédit (dépôt) sur un compte bancaire
     * Augmente le solde du compte du montant spécifié
     *
     * @param accountId Identifiant du compte à créditer
     * @param amount Montant à déposer
     * @param description Libellé de l'opération (ex: "Dépôt espèces", "Virement reçu")
     * @throws BankAccountNotFoundException Si le compte n'existe pas
     */
    void credit(String accountId, double amount, String description) throws BankAccountNotFoundException;

    /**
     * Effectue un virement entre deux comptes bancaires
     * Débite le compte source et crédite le compte destination
     *
     * @param accountIdSource Identifiant du compte à débiter
     * @param accountIdDestination Identifiant du compte à créditer
     * @param amount Montant du virement
     * @throws BankAccountNotFoundException Si l'un des comptes n'existe pas
     * @throws BalanceNotSufficientException Si le solde du compte source est insuffisant
     */
    void transfer(String accountIdSource, String accountIdDestination, double amount) throws BankAccountNotFoundException, BalanceNotSufficientException;

    /**
     * Récupère la liste de tous les comptes bancaires
     *
     * @return Liste de tous les comptes (courants et épargne)
     */
    List<BankAccountDTO> bankAccountList();

    /**
     * Récupère les détails d'un client spécifique
     *
     * @param customerId Identifiant unique du client
     * @return CustomerDTO Les informations complètes du client
     * @throws CustomerNotFoundException Si le client n'existe pas
     */
    CustomerDTO getCustomer(Long customerId) throws CustomerNotFoundException;

    /**
     * Met à jour les informations d'un client existant
     *
     * @param customerDTO Objet contenant les nouvelles informations du client
     * @return CustomerDTO Le client mis à jour
     */
    CustomerDTO updateCustomer(CustomerDTO customerDTO);

    /**
     * Supprime un client du système
     *
     * @param customerId Identifiant du client à supprimer
     * Note : Attention aux contraintes d'intégrité référentielle (comptes associés)
     */
    void deleteCustomer(Long customerId);

    /**
     * Récupère l'historique complet des opérations d'un compte
     *
     * @param accountId Identifiant du compte
     * @return Liste de toutes les opérations du compte (non paginée)
     */
    List<AccountOperationDTO> accountHistory(String accountId);

    /**
     * Récupère l'historique paginé des opérations d'un compte
     * Permet d'afficher les opérations page par page pour une meilleure performance
     *
     * @param accountId Identifiant du compte
     * @param page Numéro de la page à récupérer (commence à 0)
     * @param size Nombre d'opérations par page
     * @return AccountHistoryDTO Objet contenant les opérations de la page et les métadonnées (nombre total, pages, etc.)
     * @throws BankAccountNotFoundException Si le compte n'existe pas
     */
    AccountHistoryDTO getAccountHistory(String accountId, int page, int size) throws BankAccountNotFoundException;

    /**
     * Recherche des clients par mot-clé
     * Permet de filtrer les clients par nom ou email
     *
     * @param keyword Terme de recherche (ex: nom partiel, email)
     * @return Liste des clients correspondant au critère de recherche
     */
    List<CustomerDTO> searchCustomers(String keyword);
}