# Projet : Système de Gestion de Comptes Bancaires 
## Introduction
## 1. Contexte du Projet
Ce projet consiste à concevoir et développer une application de gestion de comptes bancaires reposant sur une architecture client–serveur. 

Il vise à appliquer les notions fondamentales du développement d’applications distribuées, notamment la modélisation orientée objet, la persistance des données, l’exposition de services web REST, la sécurité et le développement d’un client web moderne.


## 2. Description Fonctionnelle
L’application permet la gestion de clients, chacun pouvant posséder un ou plusieurs comptes bancaires.  
Chaque compte peut enregistrer plusieurs opérations bancaires de type :
- CRÉDIT
- DÉBIT

Deux types de comptes sont pris en charge :
- Compte Courant
- Compte Épargne

Les principales fonctionnalités sont :
- la gestion des clients,
- la création et la consultation des comptes bancaires,
- l’enregistrement et le suivi des opérations,
- la sécurisation de l’accès aux ressources.

## 3. Architecture de l’Application
L’architecture adoptée est organisée en plusieurs couches :
- Couche de persistance : JPA / Hibernate
- Couche métier : Services et règles de gestion
- Couche de présentation : API REST (Spring Boot)
- Couche client : Application Angular
- Couche sécurité : Spring Security et JWT

## Partie 1 : Backend (Spring Boot)
Le backend de l’application est développé à l’aide du framework Spring Boot.

### Étapes de réalisation
## Création du projet Spring Boot
J’ai commencé par créer une application Spring Boot en utilisant Spring Initializr.
  
<img width="1918" height="1020" alt="image" src="https://github.com/user-attachments/assets/4fcfa900-c770-4e41-ab48-e0b2523a945f" />

## Implémentation des entités JPA :
  
Après avoir initialisé mon projet Spring Boot, j’ai créé les **entités JPA** nécessaires à la gestion des comptes bancaires. Ces entités permettent de représenter les clients, les comptes et les opérations dans la base de données.

### Customer
J’ai défini l’entité `Customer` pour représenter les clients de la banque.  
Elle contient des informations telles que le **nom, le prénom, l’email** et une liste de comptes associés.

<img width="1918" height="1023" alt="image" src="https://github.com/user-attachments/assets/54115bfe-6f96-433f-987a-15b06d9c6da1" />

### BankAccount
L’entité `BankAccount` représente un compte bancaire générique.  
Elle contient des informations comme le **solde, la date de création, le type de compte**, et elle est reliée à un client.

<img width="1918" height="1017" alt="image" src="https://github.com/user-attachments/assets/7bf7b8bf-ec5c-4a67-9e1e-9191081bb31e" />

### SavingAccount
`SavingAccount` hérite de `BankAccount` et ajoute un **taux d’intérêt**, spécifique aux comptes d’épargne.

<img width="1918" height="1020" alt="image" src="https://github.com/user-attachments/assets/22d6d040-0ac1-44ed-bd94-eba14532196b" />

### CurrentAccount
`CurrentAccount` hérite également de `BankAccount` et inclut un **plafond de découvert autorisé**, spécifique aux comptes courants.

<img width="1918" height="1021" alt="image" src="https://github.com/user-attachments/assets/b1b75d00-24cf-4a04-9078-ffa72bdab4f0" />

### AccountOperation
L’entité `AccountOperation` représente chaque opération bancaire effectuée sur un compte.  
Elle contient le **type d’opération** (DÉBIT ou CRÉDIT), le **montant**, la **date** et la référence du compte concerné.

<img width="1917" height="1020" alt="image" src="https://github.com/user-attachments/assets/d64c4187-f772-4054-976b-f40e8a7d7e0c" />

### Remarques
J’ai utilisé les annotations **@Entity**, **@Id**, **@GeneratedValue**, ainsi que **@OneToMany** et **@ManyToOne** pour gérer les relations entre les entités, conformément aux bonnes pratiques de JPA.

## Création des interfaces Repository avec de Spring Data JPA

Après avoir défini mes entités JPA, j’ai créé les **interfaces Repository** pour chaque entité afin de faciliter l’accès aux données et la gestion de la persistance.  
J’ai utilisé **Spring Data JPA**, ce qui me permet de bénéficier automatiquement des méthodes CRUD de base sans avoir à les implémenter manuellement.

### CustomerRepository
J’ai créé `CustomerRepository` pour gérer les opérations sur l’entité `Customer`.  
Elle étend `JpaRepository<Customer, Long>` et permet de :
- Enregistrer un client
- Mettre à jour un client
- Supprimer un client
- Rechercher des clients par ID ou par email

<img width="1910" height="1017" alt="image" src="https://github.com/user-attachments/assets/25f7c548-e72a-4a89-9ca4-97132f9184f6" />

### BankAccountRepository
`BankAccountRepository` gère les comptes bancaires et étend `JpaRepository<BankAccount, String>` (en utilisant l’ID du compte).  
Elle permet de récupérer, enregistrer ou mettre à jour les comptes courants et comptes d’épargne.

<img width="1918" height="1017" alt="image" src="https://github.com/user-attachments/assets/83f7629d-1360-47d9-a596-fd0542f531c7" />

### AccountOperationRepository
`AccountOperationRepository` gère les opérations sur les comptes.  
Elle permet de récupérer la liste des opérations d’un compte donné et d’enregistrer de nouvelles opérations.

<img width="1918" height="1012" alt="image" src="https://github.com/user-attachments/assets/e89cacca-8b63-4fdb-bfba-401baf2f3021" />

### Remarques
Grâce à **Spring Data JPA**, je peux également créer des méthodes personnalisées simplement en suivant la convention de nommage de Spring (ex : `findByCustomerId(Long id)`).

## Tester la couche DAO

Après avoir créé les interfaces Repository, j’ai réalisé des **tests de la couche DAO** pour m’assurer que la persistance des données fonctionne correctement.

### Objectifs des tests
- Vérifier que les entités peuvent être enregistrées dans la base de données.
- S’assurer que les opérations CRUD (Create, Read, Update, Delete) fonctionnent correctement.
- Tester les méthodes personnalisées définies dans les repositories (ex : `findByCustomerId(Long id)`).

### Méthodologie
- J’ai utilisé **Spring Boot Test** avec l’annotation `@DataJpaTest` pour tester uniquement la couche de persistance.
- J’ai inséré des données de test dans une base de données H2 en mémoire.
- J’ai écrit des tests unitaires pour chaque repository afin de vérifier les fonctionnalités suivantes :
  - Création d’un client et d’un compte bancaire
  - Enregistrement d’opérations sur un compte
  - Lecture des données depuis la base
  - Mise à jour et suppression des enregistrements

## Couche Service et DTOs

Après avoir testé la couche DAO, j’ai mis en place la **couche Service** et les **DTOs (Data Transfer Objects)** afin de gérer la logique métier et de préparer les données à exposer via l’API REST.

La couche Service contient toutes les **règles métier** et sert d’intermédiaire entre les repositories et les contrôleurs REST.  

### Structure des services

### 1. BankService
`BankService` gère les opérations générales liées à la banque, comme la création de clients et la récupération de la liste des clients.  
Elle utilise les repositories pour interagir avec la base de données et retourne les données sous forme de DTOs.

<img width="1918" height="1016" alt="image" src="https://github.com/user-attachments/assets/79222e38-48d1-4d3c-a96a-9b47f0b8230d" />

### 2. BankAccountService
`BankAccountService` est une **interface** définissant les méthodes principales pour la gestion des comptes bancaires :  
- Création de comptes courants et comptes épargne  
- Consultation des soldes  
- Transfert et enregistrement des opérations

<img width="1918" height="1018" alt="image" src="https://github.com/user-attachments/assets/0ce57d79-8b12-448e-a6aa-a736d270c2f1" />

### 3. BankAccountServiceImpl
`BankAccountServiceImpl` est l’**implémentation** de l’interface `BankAccountService`.  
Elle contient la logique réelle pour gérer les comptes et les opérations bancaires, en utilisant les repositories et en transformant les entités en DTOs pour l’API REST.

<img width="1918" height="1017" alt="image" src="https://github.com/user-attachments/assets/32995704-f44c-43e4-8eb3-6660683cb177" />


### DTOs (Data Transfer Objects)

J’ai utilisé des **DTOs** pour transférer les données entre la couche Service et la couche Controller de manière sécurisée et efficace.  
Les DTOs permettent de **ne transmettre que les informations nécessaires au client**, sans exposer directement les entités JPA ou les données sensibles telles que les mots de passe.  

Ils facilitent également la **conversion et la transformation des données**, par exemple pour combiner plusieurs entités ou reformater les informations avant de les envoyer dans les réponses JSON de l’API REST.  

L’utilisation des DTOs contribue à **séparer la logique métier de la présentation** et à maintenir une architecture propre, sécurisée et facile à maintenir.


<img width="1918" height="1017" alt="image" src="https://github.com/user-attachments/assets/ef9b954b-88f8-4f0e-bd8d-0f869b1b2b56" />

## Développement des contrôleurs REST et tests des services RESTful

Après avoir mis en place la couche Service et les DTOs, j’ai développé les **contrôleurs REST** pour exposer les fonctionnalités de l’application via des endpoints HTTP sécurisés.  
Ces contrôleurs permettent de gérer les clients, les comptes bancaires et les opérations (CRÉDIT/DÉBIT), et de retourner les données sous forme de DTOs pour protéger les informations sensibles.

Ensuite, j’ai réalisé des **tests des services web RESTful** afin de m’assurer que les endpoints fonctionnent correctement.  
Pour cela, j’ai utilisé  **Swagger UI** pour :  
- Vérifier la création, la récupération, la mise à jour et la suppression des clients et des comptes.  
- Tester les opérations bancaires (crédit et débit) sur les comptes.  
- Contrôler que les données retournées correspondent aux DTOs et que les règles métier sont respectées.  
- Vérifier la sécurité des endpoints (authentification JWT et gestion des rôles).  

Ces tests m’ont permis de **valider l’intégrité et la fiabilité de l’API REST** avant l’intégration avec le frontend Angular.

## Partie 2 : Client Angular (Frontend)

Après avoir développé le backend avec Spring Boot, j’ai créé le **frontend avec Angular**, qui constitue l’interface utilisateur de l’application.  
Toutes les données affichées dans le frontend (clients, comptes et opérations bancaires) sont **stockées dans la base de données** via l’API REST du backend.

### Objectifs du frontend
- Permettre aux utilisateurs de gérer les clients et leurs comptes bancaires de manière intuitive.
- Afficher les informations des comptes et les opérations (CRÉDIT / DÉBIT) en temps réel depuis la base de données.
- Communiquer avec le backend via l’API REST sécurisée.
- Gérer l’authentification et l’autorisation côté client.

### Fonctionnalités principales
- Interface graphique pour la consultation et la création des clients.
- Gestion des comptes courants et épargne.
- Consultation et enregistrement des opérations bancaires.
- Intégration avec les DTOs pour ne montrer que les informations nécessaires.
- Navigation fluide entre les différentes pages de l’application.

Pour illustrer le fonctionnement du frontend, j’ai ajouté les captures suivantes :  
1. **Page de connexion** : pour montrer le formulaire d’authentification.

<img width="1918" height="1017" alt="image" src="https://github.com/user-attachments/assets/46663bce-98fa-4aa5-9580-055034d8c789" />
   
2. **Liste des clients** : affichage des clients stockés dans la base de données.

<img width="1918" height="1015" alt="image" src="https://github.com/user-attachments/assets/e7ac9027-213d-467e-afbf-2456ea04f0c9" />

3. **Formulaire de création de compte ou opération** : pour montrer la saisie et l’enregistrement de nouvelles données.
   
<img width="1918" height="1015" alt="image" src="https://github.com/user-attachments/assets/9f7f6985-655e-4edf-ac22-f16e917a4c4f" />

## Partie 3 : Sécurisation de l’application avec Spring Security et JWT

Pour protéger mon application, j’ai intégré **Spring Security** et un système d’**authentification basé sur JSON Web Tokens (JWT)**.

### Objectifs de la sécurisation
- Authentifier les utilisateurs avant d’accéder à l’API.  
- Protéger les endpoints REST et limiter l’accès selon les rôles.  
- Prévenir les accès non autorisés et sécuriser les données sensibles.

### Fonctionnement
- Lorsqu’un utilisateur se connecte, un **JWT** est généré et renvoyé au client Angular.  
- Ce token est inclus dans les requêtes HTTP suivantes pour authentifier l’utilisateur.  
- Les contrôleurs REST vérifient le token et appliquent les restrictions d’accès selon les rôles définis (ex. ROLE_USER, ROLE_ADMIN).  

<img width="1918" height="1015" alt="image" src="https://github.com/user-attachments/assets/503a7b70-23d9-4529-8fb4-203dcfbabb59" />

<img width="1918" height="1021" alt="image" src="https://github.com/user-attachments/assets/20a70362-62e0-4c68-8794-cf8badf2058f" />

## Remarques
Cette sécurisation permet de **garantir que seules les personnes autorisées peuvent accéder aux données sensibles**, tout en intégrant un mécanisme moderne et standardisé pour les applications web.

# Conclusion

Ce projet m’a permis de concevoir et de développer une application complète de gestion de comptes bancaires, intégrant à la fois un backend robuste avec Spring Boot et un frontend interactif avec Angular.  

J’ai pu appliquer les bonnes pratiques du développement logiciel, notamment :  
- La **modélisation des entités JPA** pour gérer les clients, les comptes et les opérations bancaires.  
- La mise en place d’une **couche Service** avec DTOs pour assurer la sécurité et la cohérence des données.  
- Le développement des **contrôleurs REST** et la réalisation de **tests fonctionnels** pour garantir la fiabilité de l’API.  
- La **sécurisation de l’application** avec Spring Security et JWT pour protéger les ressources et contrôler l’accès selon les rôles.  
- L’intégration d’un **frontend Angular** permettant une interface utilisateur claire, connectée à la base de données via l’API REST.

Ce projet m’a permis de renforcer mes compétences en **développement full-stack**, en **architecture en couches**, en **sécurité des applications web**, ainsi qu’en **communication entre frontend et backend**.  
Il constitue un exemple concret d’application professionnelle, fonctionnelle et sécurisée, prête à être déployée et utilisée.


