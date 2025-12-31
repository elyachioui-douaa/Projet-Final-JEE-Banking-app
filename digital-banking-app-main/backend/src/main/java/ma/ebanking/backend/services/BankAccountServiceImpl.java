package ma.ebanking.backend.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.ebanking.backend.dto.*;
import ma.ebanking.backend.entities.*;
import ma.ebanking.backend.enums.OperationType;
import ma.ebanking.backend.exceptions.BalanceNotSufficientException;
import ma.ebanking.backend.exceptions.BankAccountNotFoundException;
import ma.ebanking.backend.exceptions.CustomerNotFoundException;
import ma.ebanking.backend.mappers.BankAccountMapperImpl;
import ma.ebanking.backend.repositories.AccountOperationRepository;
import ma.ebanking.backend.repositories.BankAccountRepository;
import ma.ebanking.backend.repositories.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implémentation du service de gestion des comptes bancaires
 * Cette classe contient toute la logique métier de l'application bancaire
 */
@Service // Indique que cette classe est un composant de service Spring
@Transactional // Toutes les méthodes de cette classe sont exécutées dans une transaction (rollback automatique en cas d'erreur)
@AllArgsConstructor // Génère un constructeur avec tous les paramètres (injection de dépendances)
@Slf4j // Active le logging avec SLF4J (permet d'utiliser log.info(), log.error(), etc.)
public class BankAccountServiceImpl implements BankAccountService {

    // Injection des dépendances via le constructeur (généré par @AllArgsConstructor)
    private CustomerRepository customerRepository;
    private BankAccountRepository bankAccountRepository;
    private AccountOperationRepository accountOperationRepository;
    private BankAccountMapperImpl dtoMapper; // Mapper pour convertir entre entités et DTOs

    /**
     * Enregistre un nouveau client dans la base de données
     *
     * @param customerDTO Objet DTO contenant les informations du client
     * @return Le client sauvegardé sous forme de DTO
     */
    @Override
    public CustomerDTO saveCustomer(CustomerDTO customerDTO) {
        log.info("Saving new Customer"); // Log pour tracer l'opération
        Customer customer = dtoMapper.fromCustomerDTO(customerDTO); // Conversion DTO -> Entité
        Customer savedCustomer = customerRepository.save(customer); // Sauvegarde en base
        return dtoMapper.fromCustomer(savedCustomer); // Conversion Entité -> DTO pour le retour
    }

    /**
     * Crée un nouveau compte courant avec découvert autorisé
     *
     * @param initialBalance Solde initial du compte
     * @param overDraft Montant du découvert autorisé
     * @param customerId Identifiant du client propriétaire
     * @return Le compte courant créé sous forme de DTO
     * @throws CustomerNotFoundException Si le client n'existe pas
     */
    @Override
    public CurrentBankAccountDTO saveCurrentBankAccount(double initialBalance, double overDraft, Long customerId) throws CustomerNotFoundException {
        // Recherche du client dans la base de données
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null)
            throw new CustomerNotFoundException("Customer not found");

        // Création et initialisation du compte courant
        CurrentAccount currentAccount = new CurrentAccount();
        currentAccount.setId(UUID.randomUUID().toString()); // Génération d'un identifiant unique
        currentAccount.setCreatedAt(new Date()); // Date de création
        currentAccount.setBalance(initialBalance);
        currentAccount.setOverDraft(overDraft); // Spécifique au compte courant
        currentAccount.setCustomer(customer); // Association avec le client

        // Sauvegarde et retour du DTO
        CurrentAccount savedBankAccount = bankAccountRepository.save(currentAccount);
        return dtoMapper.fromCurrentBankAccount(savedBankAccount);
    }

    /**
     * Crée un nouveau compte épargne avec taux d'intérêt
     *
     * @param initialBalance Solde initial du compte
     * @param interestRate Taux d'intérêt annuel (ex: 0.05 pour 5%)
     * @param customerId Identifiant du client propriétaire
     * @return Le compte épargne créé sous forme de DTO
     * @throws CustomerNotFoundException Si le client n'existe pas
     */
    @Override
    public SavingBankAccountDTO saveSavingBankAccount(double initialBalance, double interestRate, Long customerId) throws CustomerNotFoundException {
        // Recherche du client
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null)
            throw new CustomerNotFoundException("Customer not found");

        // Création et initialisation du compte épargne
        SavingAccount savingAccount = new SavingAccount();
        savingAccount.setId(UUID.randomUUID().toString());
        savingAccount.setCreatedAt(new Date());
        savingAccount.setBalance(initialBalance);
        savingAccount.setInterestRate(interestRate); // Spécifique au compte épargne
        savingAccount.setCustomer(customer);

        // Sauvegarde et retour du DTO
        SavingAccount savedBankAccount = bankAccountRepository.save(savingAccount);
        return dtoMapper.fromSavingBankAccount(savedBankAccount);
    }

    /**
     * Récupère la liste de tous les clients
     *
     * @return Liste des clients sous forme de DTOs
     */
    @Override
    public List<CustomerDTO> listCustomers() {
        List<Customer> customers = customerRepository.findAll();
        // Utilisation de Stream API pour convertir chaque entité en DTO
        List<CustomerDTO> customerDTOS = customers.stream()
                .map(customer -> dtoMapper.fromCustomer(customer))
                .collect(Collectors.toList());
        return customerDTOS;
    }

    /**
     * Récupère les informations d'un compte bancaire spécifique
     * Gère le polymorphisme entre compte courant et compte épargne
     *
     * @param accountId Identifiant du compte
     * @return DTO du compte (CurrentBankAccountDTO ou SavingBankAccountDTO)
     * @throws BankAccountNotFoundException Si le compte n'existe pas
     */
    @Override
    public BankAccountDTO getBankAccount(String accountId) throws BankAccountNotFoundException {
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("BankAccount not found"));

        // Vérification du type réel du compte et conversion appropriée
        if (bankAccount instanceof SavingAccount) {
            SavingAccount savingAccount = (SavingAccount) bankAccount;
            return dtoMapper.fromSavingBankAccount(savingAccount);
        } else {
            CurrentAccount currentAccount = (CurrentAccount) bankAccount;
            return dtoMapper.fromCurrentBankAccount(currentAccount);
        }
    }

    /**
     * Effectue un débit (retrait) sur un compte bancaire
     * Vérifie la disponibilité des fonds avant l'opération
     *
     * @param accountId Identifiant du compte à débiter
     * @param amount Montant à retirer
     * @param description Description de l'opération
     * @throws BankAccountNotFoundException Si le compte n'existe pas
     * @throws BalanceNotSufficientException Si le solde est insuffisant
     */
    @Override
    public void debit(String accountId, double amount, String description) throws BankAccountNotFoundException, BalanceNotSufficientException {
        // Récupération du compte
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("BankAccount not found"));

        // Vérification du solde suffisant
        if (bankAccount.getBalance() < amount)
            throw new BalanceNotSufficientException("Balance not sufficient");

        // Création de l'opération de débit
        AccountOperation accountOperation = new AccountOperation();
        accountOperation.setType(OperationType.DEBIT);
        accountOperation.setAmount(amount);
        accountOperation.setDescription(description);
        accountOperation.setOperationDate(new Date());
        accountOperation.setBankAccount(bankAccount);
        accountOperationRepository.save(accountOperation);

        // Mise à jour du solde du compte
        bankAccount.setBalance(bankAccount.getBalance() - amount);
        bankAccountRepository.save(bankAccount);
    }

    /**
     * Effectue un crédit (dépôt) sur un compte bancaire
     *
     * @param accountId Identifiant du compte à créditer
     * @param amount Montant à déposer
     * @param description Description de l'opération
     * @throws BankAccountNotFoundException Si le compte n'existe pas
     */
    @Override
    public void credit(String accountId, double amount, String description) throws BankAccountNotFoundException {
        // Récupération du compte
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("BankAccount not found"));

        // Création de l'opération de crédit
        AccountOperation accountOperation = new AccountOperation();
        accountOperation.setType(OperationType.CREDIT);
        accountOperation.setAmount(amount);
        accountOperation.setDescription(description);
        accountOperation.setOperationDate(new Date());
        accountOperation.setBankAccount(bankAccount);
        accountOperationRepository.save(accountOperation);

        // Mise à jour du solde du compte (addition)
        bankAccount.setBalance(bankAccount.getBalance() + amount);
        bankAccountRepository.save(bankAccount);
    }

    /**
     * Effectue un virement entre deux comptes
     * Utilise les méthodes debit() et credit() pour garantir la cohérence
     *
     * @param accountIdSource Compte source (débité)
     * @param accountIdDestination Compte destination (crédité)
     * @param amount Montant du virement
     * @throws BankAccountNotFoundException Si l'un des comptes n'existe pas
     * @throws BalanceNotSufficientException Si le solde du compte source est insuffisant
     */
    @Override
    public void transfer(String accountIdSource, String accountIdDestination, double amount) throws BankAccountNotFoundException, BalanceNotSufficientException {
        // Débit du compte source avec description du destinataire
        debit(accountIdSource, amount, "Transfer to " + accountIdDestination);
        // Crédit du compte destination avec description de l'expéditeur
        credit(accountIdDestination, amount, "Transfer from " + accountIdSource);
        // Note : @Transactional garantit que les deux opérations sont atomiques (tout ou rien)
    }

    /**
     * Récupère la liste de tous les comptes bancaires
     * Gère le polymorphisme entre comptes courants et comptes épargne
     *
     * @return Liste de tous les comptes sous forme de DTOs
     */
    @Override
    public List<BankAccountDTO> bankAccountList() {
        List<BankAccount> bankAccounts = bankAccountRepository.findAll();
        // Conversion avec gestion du type de compte
        List<BankAccountDTO> bankAccountDTOS = bankAccounts.stream().map(bankAccount -> {
            if (bankAccount instanceof SavingAccount) {
                SavingAccount savingAccount = (SavingAccount) bankAccount;
                return dtoMapper.fromSavingBankAccount(savingAccount);
            } else {
                CurrentAccount currentAccount = (CurrentAccount) bankAccount;
                return dtoMapper.fromCurrentBankAccount(currentAccount);
            }
        }).collect(Collectors.toList());
        return bankAccountDTOS;
    }

    /**
     * Récupère les informations d'un client spécifique
     *
     * @param customerId Identifiant du client
     * @return Informations du client sous forme de DTO
     * @throws CustomerNotFoundException Si le client n'existe pas
     */
    @Override
    public CustomerDTO getCustomer(Long customerId) throws CustomerNotFoundException {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer Not found"));
        return dtoMapper.fromCustomer(customer);
    }

    /**
     * Met à jour les informations d'un client existant
     *
     * @param customerDTO Nouvelles informations du client
     * @return Le client mis à jour sous forme de DTO
     */
    @Override
    public CustomerDTO updateCustomer(CustomerDTO customerDTO) {
        log.info("Saving new Customer"); // Note : ce message devrait être "Updating Customer"
        Customer customer = dtoMapper.fromCustomerDTO(customerDTO);
        Customer savedCustomer = customerRepository.save(customer); // save() fait un update si l'ID existe
        return dtoMapper.fromCustomer(savedCustomer);
    }

    /**
     * Supprime un client de la base de données
     *
     * @param customerId Identifiant du client à supprimer
     * Note : Attention aux contraintes d'intégrité si le client a des comptes associés
     */
    @Override
    public void deleteCustomer(Long customerId) {
        customerRepository.deleteById(customerId);
    }

    /**
     * Récupère l'historique complet des opérations d'un compte
     *
     * @param accountId Identifiant du compte
     * @return Liste de toutes les opérations du compte
     */
    @Override
    public List<AccountOperationDTO> accountHistory(String accountId) {
        List<AccountOperation> accountOperations = accountOperationRepository.findByBankAccountId(accountId);
        // Conversion des entités en DTOs
        return accountOperations.stream()
                .map(op -> dtoMapper.fromAccountOperation(op))
                .collect(Collectors.toList());
    }

    /**
     * Récupère l'historique paginé des opérations d'un compte
     * Inclut les métadonnées de pagination et les informations du compte
     *
     * @param accountId Identifiant du compte
     * @param page Numéro de la page (commence à 0)
     * @param size Nombre d'éléments par page
     * @return Objet contenant les opérations, le solde et les infos de pagination
     * @throws BankAccountNotFoundException Si le compte n'existe pas
     */
    @Override
    public AccountHistoryDTO getAccountHistory(String accountId, int page, int size) throws BankAccountNotFoundException {
        // Vérification de l'existence du compte
        BankAccount bankAccount = bankAccountRepository.findById(accountId).orElse(null);
        if (bankAccount == null)
            throw new BankAccountNotFoundException("Account not Found");

        // Récupération des opérations paginées, triées par date décroissante
        Page<AccountOperation> accountOperations = accountOperationRepository
                .findByBankAccountIdOrderByOperationDateDesc(accountId, PageRequest.of(page, size));

        // Construction du DTO de réponse
        AccountHistoryDTO accountHistoryDTO = new AccountHistoryDTO();

        // Conversion des opérations de la page courante
        List<AccountOperationDTO> accountOperationDTOS = accountOperations.getContent().stream()
                .map(op -> dtoMapper.fromAccountOperation(op))
                .collect(Collectors.toList());

        // Remplissage des informations
        accountHistoryDTO.setAccountOperationDTOS(accountOperationDTOS);
        accountHistoryDTO.setAccountId(bankAccount.getId());
        accountHistoryDTO.setBalance(bankAccount.getBalance()); // Solde actuel du compte
        accountHistoryDTO.setCurrentPage(page);
        accountHistoryDTO.setPageSize(size);
        accountHistoryDTO.setTotalPages(accountOperations.getTotalPages()); // Nombre total de pages

        return accountHistoryDTO;
    }

    /**
     * Recherche des clients par mot-clé (nom ou email)
     *
     * @param keyword Terme de recherche
     * @return Liste des clients correspondant au critère
     */
    @Override
    public List<CustomerDTO> searchCustomers(String keyword) {
        // Délègue la recherche au repository (requête personnalisée)
        List<Customer> customers = customerRepository.searchCustomer(keyword);
        // Conversion en DTOs
        List<CustomerDTO> customerDTOS = customers.stream()
                .map(cust -> dtoMapper.fromCustomer(cust))
                .collect(Collectors.toList());
        return customerDTOS;
    }
}