# 📝 Système de Fichier Texte pour les Utilisateurs

## ✅ Ce qui a été implémenté

J'ai créé un système simple qui sauvegarde les utilisateurs dans un fichier texte local (`users.txt`) au lieu d'utiliser MongoDB.

### Fonctionnalités :

✅ **Inscription (Sign Up)** : Sauvegarde l'utilisateur dans `users.txt`
✅ **Connexion (Sign In)** : Vérifie l'email et le mot de passe depuis le fichier
✅ **Vérification du rôle** : Redirige vers ParentHome ou ChildHome selon le rôle
✅ **Pas de doublon** : Vérifie que l'email n'existe pas déjà

---

## 📄 Format du fichier users.txt

Le fichier est créé automatiquement dans le stockage interne de l'application.

**Emplacement** : `/data/data/com.example.dam_android/files/users.txt`

**Format** : Chaque ligne représente un utilisateur
```
email|name|password|role
```

**Exemple** :
```
parent@test.com|Mohamed Amin|password123|PARENT
child@test.com|Chaima|pass456|CHILD
john@example.com|John Doe|mypass|PARENT
```

---

## 🔄 Flux de l'application

### Inscription (Sign Up)
```
1. Utilisateur remplit le formulaire
2. Sélectionne "Parent" ou "Child"
3. Click "Sign Up"
   ↓
4. Vérification : Email existe déjà ?
   - OUI → Message d'erreur "Email déjà utilisé"
   - NON → Continue
   ↓
5. Ajout dans users.txt : email|name|password|role
6. Sauvegarde de la session
7. Redirection automatique :
   - PARENT → ParentHomeFragment
   - CHILD → ChildHomeFragment
```

### Connexion (Sign In)
```
1. Utilisateur entre email et password
2. Click "Sign In"
   ↓
3. Lecture du fichier users.txt
4. Vérification ligne par ligne :
   - Email correspond ? ET Password correspond ?
   ↓
5. Si trouvé :
   - Récupération du rôle (PARENT ou CHILD)
   - Sauvegarde de la session
   - Redirection selon le rôle :
     * PARENT → ParentHomeFragment (liste des enfants)
     * CHILD → ChildHomeFragment (carte + bouton danger)
   ↓
6. Si non trouvé :
   - Message : "Email ou mot de passe incorrect"
```

---

## 🧪 Comment tester

### Test 1 : Créer un compte Parent
1. Lancez l'application
2. Allez sur "Sign Up"
3. Remplissez :
   - Name : `Parent Test`
   - Email : `parent@test.com`
   - Password : `123456`
   - Confirm : `123456`
   - Sélectionnez : **Parent**
4. Cliquez "Sign Up"
5. ✅ Vous êtes redirigé vers **ParentHome** (liste des enfants)

### Test 2 : Créer un compte Child
1. Déconnectez-vous (si nécessaire)
2. Allez sur "Sign Up"
3. Remplissez :
   - Name : `Child Test`
   - Email : `child@test.com`
   - Password : `123456`
   - Confirm : `123456`
   - Sélectionnez : **Child**
4. Cliquez "Sign Up"
5. ✅ Vous êtes redirigé vers **ChildHome** (carte + alerte)

### Test 3 : Connexion
1. Déconnectez-vous
2. Allez sur "Sign In"
3. Entrez :
   - Email : `parent@test.com`
   - Password : `123456`
4. Cliquez "Sign In"
5. ✅ Vous êtes redirigé vers **ParentHome**

### Test 4 : Vérifier le fichier
Pour voir le contenu du fichier `users.txt` :

**Méthode 1 - Android Studio Device File Explorer :**
1. View → Tool Windows → Device File Explorer
2. Naviguez vers : `/data/data/com.example.dam_android/files/`
3. Trouvez `users.txt`
4. Double-cliquez pour le voir

**Méthode 2 - Ajouter un bouton de debug :**
Vous pouvez ajouter temporairement un bouton dans ParentHome pour afficher le contenu :
```kotlin
// Dans ParentHomeFragment
val userFileManager = UserFileManager.getInstance(requireContext())
val allUsers = userFileManager.getAllUsers()
Toast.makeText(context, "Users: ${allUsers.size}", Toast.LENGTH_LONG).show()
```

---

## 📊 Avantages de cette approche

✅ **Simple** : Pas besoin de serveur ou base de données
✅ **Rapide** : Lecture/écriture instantanée
✅ **Hors ligne** : Fonctionne sans Internet
✅ **Léger** : Pas de dépendances externes

## ⚠️ Limitations

❌ **Mot de passe en clair** : Non sécurisé (pour production, hashage requis)
❌ **Local uniquement** : Données perdues si app désinstallée
❌ **Pas de synchronisation** : Ne fonctionne pas entre appareils
❌ **Performance** : Lent avec beaucoup d'utilisateurs (>1000)

---

## 🔧 Méthodes disponibles

### UserFileManager

```kotlin
// Sauvegarder un utilisateur
userFileManager.saveUser(user) // Retourne true si succès

// Authentifier un utilisateur
val user = userFileManager.authenticateUser(email, password) // Retourne User ou null

// Vérifier si un email existe
val exists = userFileManager.userExists(email) // Retourne true/false

// Obtenir tous les utilisateurs
val allUsers = userFileManager.getAllUsers() // Retourne List<User>

// Supprimer tous les utilisateurs (reset)
userFileManager.clearAllUsers()

// Obtenir le chemin du fichier
val path = userFileManager.getFilePath()
```

---

## 🚀 Exemple de contenu du fichier après quelques inscriptions

```
parent1@gmail.com|Ahmed Ben Ali|password123|PARENT
child1@gmail.com|Sara|kid123|CHILD
parent2@yahoo.fr|Fatima|mypass456|PARENT
child2@hotmail.com|Mohamed|child789|CHILD
```

Chaque ligne = 1 utilisateur
Format : `email|nom|password|role`

---

## 🎯 Ce qui fonctionne maintenant

✅ **Sign Up** → Sauvegarde dans users.txt + Redirection selon rôle
✅ **Sign In** → Lecture de users.txt + Vérification + Redirection selon rôle
✅ **Session** → Utilisateur reste connecté même après fermeture de l'app
✅ **Rôles** → Parent voit ParentHome, Child voit ChildHome
✅ **Validation** → Pas de doublons d'email

---

## 📱 Tester maintenant !

1. Lancez l'application
2. Créez 2 comptes : 1 Parent et 1 Child
3. Déconnectez-vous et reconnectez-vous avec chaque compte
4. Vérifiez que la redirection fonctionne correctement selon le rôle

**Tout fonctionne ! 🎉**

