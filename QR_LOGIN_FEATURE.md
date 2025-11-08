# Feature: Connexion Enfant avec QR Code

## Modifications apportées

### 1. Dépendances ajoutées (app/build.gradle.kts)
- CameraX (camera-core, camera-camera2, camera-lifecycle, camera-view) version 1.3.1
- ML Kit Barcode Scanning version 17.2.0

### 2. Permissions (AndroidManifest.xml)
- `android.permission.CAMERA` - Pour accéder à la caméra
- `android.hardware.camera` feature (non obligatoire)

### 3. Nouveaux fichiers créés

#### LoginChildQrScreen.kt
Écran de scan QR pour la connexion des enfants avec :
- Demande de permission caméra
- Aperçu caméra en temps réel avec CameraX
- Détection de QR codes avec ML Kit
- Interface utilisateur cohérente avec le design de l'app
- Gestion des erreurs et états de chargement

### 4. API Backend

#### AuthApi.kt
Ajout de l'endpoint :
```kotlin
@POST("auth/qrcode/login")
suspend fun loginWithQr(@Body request: QrLoginRequest): LoginResponse
```

#### ApiDtos.kt
Nouveau DTO :
```kotlin
data class QrLoginRequest(
    val qrCode: String
)
```

#### ApiService.kt
Nouvelle méthode :
```kotlin
suspend fun loginChildWithQr(qrCode: String): Result<Pair<User, String>>
```

### 5. Navigation
# Feature: Connexion Enfant avec QR Code
#### NavRoutes.kt
Ajout de la route :
```kotlin
const val LOGIN_CHILD_QR = "login_child_qr"
```

#### MainActivity.kt
Ajout de la navigation vers l'écran de scan QR

#### SignInScreen.kt
Ajout du bouton "Sign in as child" qui redirige vers l'écran de scan QR

## Comment utiliser

1. **Synchroniser Gradle** : Ouvrez Android Studio et synchronisez le projet pour installer les nouvelles dépendances
2. **Build** : Compilez le projet
3. **Test** : 
   - Depuis l'écran de connexion, cliquez sur "Sign in as child"
   - Autorisez l'accès à la caméra
   - Scannez le QR code d'un enfant
   - L'enfant sera automatiquement connecté et redirigé vers son écran d'accueil

## Flux utilisateur

```
SignInScreen
    ↓ [Clic sur "Sign in as child"]
LoginChildQrScreen
    ↓ [Permission caméra accordée]
[Aperçu caméra + détection QR]
    ↓ [QR code scanné]
[Appel API auth/qrcode/login]
    ↓ [Connexion réussie]
ChildHomeScreen
```

## Notes importantes

- Le QR code doit être valide et correspondre à un enfant enregistré dans le système
- La caméra se lance automatiquement après autorisation
- Le scan est automatique dès qu'un QR code est détecté
- Un indicateur de chargement s'affiche pendant la connexion
- Les erreurs sont affichées clairement à l'utilisateur

## Modifications apportées

### 1. Dépendances ajoutées (app/build.gradle.kts)
- CameraX (camera-core, camera-camera2, camera-lifecycle, camera-view) version 1.3.1
- ML Kit Barcode Scanning version 17.2.0

### 2. Permissions (AndroidManifest.xml)
- `android.permission.CAMERA` - Pour accéder à la caméra
- `android.hardware.camera` feature (non obligatoire)

### 3. Nouveaux fichiers créés

#### LoginChildQrScreen.kt
Écran de scan QR pour la connexion des enfants avec :
- Demande de permission caméra
- Aperçu caméra en temps réel avec CameraX
- Détection de QR codes avec ML Kit
- Interface utilisateur cohérente avec le design de l'app
- Gestion des erreurs et états de chargement

### 4. API Backend

#### AuthApi.kt
Ajout de l'endpoint :
```kotlin
@POST("auth/qrcode/login")
suspend fun loginWithQr(@Body request: QrLoginRequest): LoginResponse
```

#### ApiDtos.kt
Nouveau DTO :
```kotlin
data class QrLoginRequest(
    val qrCode: String
)
```

#### ApiService.kt
Nouvelle méthode :
```kotlin
suspend fun loginChildWithQr(qrCode: String): Result<Pair<User, String>>
```

### 5. Navigation

#### NavRoutes.kt
Ajout de la route :
```kotlin
const val LOGIN_CHILD_QR = "login_child_qr"
```

#### MainActivity.kt
Ajout de la navigation vers l'écran de scan QR

#### SignInScreen.kt
Ajout du bouton "Sign in as child" qui redirige vers l'écran de scan QR

## Comment utiliser

1. **Synchroniser Gradle** : Ouvrez Android Studio et synchronisez le projet pour installer les nouvelles dépendances
2. **Build** : Compilez le projet
3. **Test** : 
   - Depuis l'écran de connexion, cliquez sur "Sign in as child"
   - Autorisez l'accès à la caméra
   - Scannez le QR code d'un enfant
   - L'enfant sera automatiquement connecté et redirigé vers son écran d'accueil

## Flux utilisateur

```
SignInScreen
    ↓ [Clic sur "Sign in as child"]
LoginChildQrScreen
    ↓ [Permission caméra accordée]
[Aperçu caméra + détection QR]
    ↓ [QR code scanné]
[Appel API auth/qrcode/login]
    ↓ [Connexion réussie]
ChildHomeScreen
```

## Notes importantes

- Le QR code doit être valide et correspondre à un enfant enregistré dans le système
- La caméra se lance automatiquement après autorisation
- Le scan est automatique dès qu'un QR code est détecté
- Un indicateur de chargement s'affiche pendant la connexion
- Les erreurs sont affichées clairement à l'utilisateur
