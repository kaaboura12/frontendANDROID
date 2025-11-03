# Architecture MVVM - DAM Android

## Structure du projet

Le projet utilise l'architecture **MVVM (Model-View-ViewModel)** qui sépare clairement les responsabilités :

```
app/src/main/java/com/example/dam_android/
├── data/
│   ├── model/          # Modèles de données
│   │   ├── User.kt
│   │   └── AuthResult.kt
│   └── repository/     # Couche d'accès aux données
│       └── AuthRepository.kt
├── viewmodel/          # ViewModels (logique métier)
│   ├── SignInViewModel.kt
│   ├── SignUpViewModel.kt
│   ├── ForgotPasswordViewModel.kt
│   └── ViewModelFactory.kt
├── ui/                 # Fragments (Views)
│   ├── SignInFragment.kt
│   ├── SignUpFragment.kt
│   ├── ForgotPasswordFragment.kt
│   ├── SplashFragment.kt
│   └── WelcomeFragment.kt
└── MainActivity.kt
```

## Les 3 couches de MVVM

### 1. **Model (Modèle de données)**
- **User.kt** : Représente un utilisateur
- **AuthResult.kt** : Sealed class pour gérer les états d'authentification (Success, Error, Loading)

### 2. **View (Vue - Fragments)**
- Responsables uniquement de l'affichage et des interactions utilisateur
- Observent les LiveData du ViewModel
- Ne contiennent pas de logique métier

### 3. **ViewModel**
- Contient la logique métier
- Expose des LiveData pour communiquer avec la View
- Survit aux changements de configuration (rotation d'écran)
- Communique avec le Repository pour les données

### 4. **Repository**
- Centralise l'accès aux données (API, base de données locale, etc.)
- Pattern Singleton pour une instance unique
- Utilise Kotlin Flow pour les opérations asynchrones

## Fonctionnalités implémentées

### SignInViewModel
- Validation en temps réel de l'email et du mot de passe
- Gestion de l'état du bouton de connexion
- Authentification avec gestion des états (Loading, Success, Error)

### SignUpViewModel
- Validation des champs : nom, email, mot de passe, confirmation
- Vérification que les mots de passe correspondent
- Inscription avec feedback utilisateur

### ForgotPasswordViewModel
- Validation de l'email
- Envoi de lien de réinitialisation
- Gestion des erreurs

## Avantages de cette architecture

✅ **Séparation des responsabilités** : Chaque classe a un rôle bien défini
✅ **Testabilité** : Les ViewModels peuvent être testés indépendamment
✅ **Réutilisabilité** : Le Repository peut être partagé entre plusieurs ViewModels
✅ **Maintenabilité** : Code plus facile à maintenir et à faire évoluer
✅ **Survie aux changements de configuration** : Les données survivent à la rotation

## Utilisation

### Dans un Fragment :

```kotlin
class SignInFragment : Fragment() {
    private val viewModel: SignInViewModel by viewModels {
        ViewModelFactory(AuthRepository.getInstance())
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Loading -> // Afficher loading
                is AuthResult.Success -> // Navigation
                is AuthResult.Error -> // Afficher erreur
            }
        }
    }
}
```

## Dépendances utilisées

- `androidx.lifecycle.viewmodel.ktx` : ViewModels
- `androidx.lifecycle.livedata.ktx` : LiveData
- `kotlinx.coroutines` : Coroutines pour les opérations asynchrones
- `androidx.fragment.ktx` : Extensions pour faciliter l'utilisation des ViewModels

## Prochaines étapes

- [ ] Ajouter Room Database pour la persistance locale
- [ ] Intégrer Retrofit pour les appels API
- [ ] Ajouter des tests unitaires pour les ViewModels
- [ ] Implémenter SharedPreferences pour la session utilisateur
- [ ] Ajouter Hilt/Dagger pour l'injection de dépendances

