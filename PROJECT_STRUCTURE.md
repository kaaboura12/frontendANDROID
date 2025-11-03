# 📁 Architecture du Projet - MVVM

## Structure Actuelle

```
app/src/main/java/com/example/dam_android/
│
├── 📦 data/
│   ├── model/
│   │   ├── User.kt
│   │   ├── AuthResult.kt
│   │   └── FormValidation.kt
│   │
│   └── repository/
│       └── AuthRepository.kt
│
├── 📦 viewmodel/
│   ├── SignInViewModel.kt
│   ├── SignUpViewModel.kt
│   ├── ForgotPasswordViewModel.kt
│   └── ViewModelFactory.kt
│
├── 📦 ui/
│   ├── splash/
│   │   └── SplashFragment.kt
│   │
│   ├── welcome/
│   │   └── WelcomeFragment.kt
│   │
│   ├── signin/
│   │   └── SignInFragment.kt
│   │
│   ├── signup/
│   │   └── SignUpFragment.kt
│   │
│   ├── forgotpassword/
│   │   └── ForgotPasswordFragment.kt
│   │
│   ├── home/
│   │   └── HomeFragment.kt
│   │
│   ├── gallery/
│   │   └── GalleryFragment.kt
│   │
│   └── slideshow/
│       ├── SlideshowFragment.kt
│       └── SlideshowViewModel.kt
│
└── MainActivity.kt
```

## ✅ Modifications Effectuées

### Fragments Réorganisés :
- ✅ `SplashFragment.kt` → `ui/splash/`
- ✅ `WelcomeFragment.kt` → `ui/welcome/`
- ✅ `SignInFragment.kt` → `ui/signin/`
- ✅ `SignUpFragment.kt` → `ui/signup/`
- ✅ `ForgotPasswordFragment.kt` → `ui/forgotpassword/`

### Fichier de Navigation Mis à Jour :
- ✅ `mobile_navigation.xml` - Tous les chemins des fragments ont été mis à jour

### Packages Créés :
- `ui.splash`
- `ui.welcome`
- `ui.signin`
- `ui.signup`
- `ui.forgotpassword`

## 🔄 Prochaines Étapes

### À Faire Manuellement :
1. **Supprimer les anciens fichiers** dans `ui/` (racine) :
   - `ui/SplashFragment.kt` (ancien)
   - `ui/WelcomeFragment.kt` (ancien)
   - `ui/SignInFragment.kt` (ancien)
   - `ui/SignUpFragment.kt` (ancien)
   - `ui/ForgotPasswordFragment.kt` (ancien)

2. **Faire un Gradle Sync** :
   - Cliquez sur l'icône 🐘 dans la barre d'outils
   - Ou : `File > Sync Project with Gradle Files`

3. **Rebuild le projet** :
   - `Build > Rebuild Project`

## 📊 Architecture MVVM

```
┌─────────────────────────────────────────────────┐
│                    View Layer                    │
│              (Fragments dans ui/)                │
│   splash/  welcome/  signin/  signup/  forgot/  │
└───────────────────┬─────────────────────────────┘
                    │ observes LiveData
                    ↓
┌─────────────────────────────────────────────────┐
│                 ViewModel Layer                  │
│            (ViewModels + Factory)                │
│  SignInViewModel, SignUpViewModel, etc.         │
└───────────────────┬─────────────────────────────┘
                    │ uses
                    ↓
┌─────────────────────────────────────────────────┐
│               Repository Layer                   │
│              (AuthRepository)                    │
│     Single source of truth for data             │
└───────────────────┬─────────────────────────────┘
                    │ accesses
                    ↓
┌─────────────────────────────────────────────────┐
│                  Model Layer                     │
│        (Data classes: User, AuthResult)         │
└─────────────────────────────────────────────────┘
```

## 🎯 Avantages de cette Structure

✅ **Séparation claire** - Chaque feature dans son propre package
✅ **Facilité de navigation** - Facile de trouver les fichiers
✅ **Scalable** - Facile d'ajouter de nouvelles features
✅ **Maintenable** - Code organisé et structuré
✅ **Testable** - Chaque couche peut être testée indépendamment

## 📝 Convention de Nommage

- **Packages** : lowercase (splash, signin, signup)
- **Classes** : PascalCase (SignInFragment, SignInViewModel)
- **Fichiers** : Même nom que la classe

---
*Architecture mise à jour le : 2025-01-02*

