# Architecture Map — music-app (Android / Jetpack Compose)

> Mappa di riferimento rapido per sessioni di sviluppo. Aggiornare quando si aggiungono file strutturali.

---

## 1. Struttura cartelle

```
music-app/
├── app/src/main/java/com/example/
│   ├── data/           # Repository, API client, sorgenti audio
│   ├── model/          # Data class condivise (Models.kt unico file)
│   ├── ui/
│   │   ├── components/ # Composable riusabili (badge, dialog, card…)
│   │   ├── screens/    # Schermate complete (una per file)
│   │   └── theme/      # Color.kt · Type.kt · Theme.kt
│   └── viewmodel/      # MusicViewModel (unico ViewModel dell'app)
├── app/src/main/res/   # drawable, layout (non usato), values
├── scripts/            # Script CI/firestore (non committati se contengono secrets)
├── .github/workflows/  # CI: build-apk.yml
└── app/build.gradle.kts
```

---

## 2. Entry point e navigazione

| File | Ruolo |
|---|---|
| `MainActivity.kt` | Activity unica. Ospita tutto il Compose tree. Gestisce `BackHandler`, navigazione a pannelli sovrapposti (niente NavGraph), Coil setup, broadcast receiver Spotify. |
| `AppChangelog.kt` | `TITLE` + `LINES` della versione corrente — mostrati una sola volta al login dopo aggiornamento. |
| `AppFirebaseMessagingService.kt` | FCM: riceve push e aggiorna Firestore della live corrente. |
| `MusicNotificationListenerService.kt` | `NotificationListenerService`: legge notifiche di Spotify Free / Amazon Music e ne estrae il brano in ascolto. |

### Flusso di navigazione (no NavGraph)

```
MainActivity
 └─ MusicUiState.isLoggedIn ?
     ├─ false → LoginScreen
     └─ true  → MainFeedScreen (pagina base)
                 ├─ LiveDetailScreen    (slide up, pannello sopra)
                 ├─ ProfileScreen       (slide up, pannello sopra)
                 ├─ ChatListScreen      (slide up, pannello sopra)
                 ├─ ChatScreen          (slide up, pannello sopra)
                 ├─ TrackDetailDialog   (dialog modale)
                 ├─ NotificationsDialog (dialog modale)
                 ├─ PeopleSearchDialog  (dialog modale)
                 └─ NowPlayingSheet     (bottom sheet)
```

Ogni pannello è attivato da un flag in `MusicUiState` (es. `activeProfileUser != null`, `activeChatUser != null`). La navigazione è completamente custom senza NavigationComponent.

---

## 3. Data layer

| File | Ruolo |
|---|---|
| `data/FirebaseRepository.kt` | Listener Firestore real-time per feed utenti, chat, notifiche, friend request, heartedKeys. Pubblica dati via callback/Flow. |
| `data/AuthRepository.kt` | Google Sign-In + Firebase Auth. `signIn()`, `signOut()`, `currentUser`. |
| `data/SpotifyRepository.kt` | Polling ogni ~5s al Coordinator per brano corrente (Spotify Free via notifiche). |
| `data/SpotifyWebApiRepository.kt` | Chiama Web API Spotify (`/me/player/currently-playing`, `/v1/artists/{id}` per genere). Usa `accessToken` da `SpotifyAuthRepository`. |
| `data/SpotifyAuthRepository.kt` | OAuth PKCE Spotify: scambia code, refresha token, persiste in `SharedPreferences`. |
| `data/SpotifyAppRemoteSource.kt` | SDK App Remote (attualmente disattivato per bug lifecycle — usare listener/Web API). |
| `data/MusicRepository.kt` | Wrapper orchestratore: combina sorgenti Spotify e pubblica il brano live su Firestore. |
| `data/CoverResolver.kt` | Risolve cover URL da notifiche Android (content:// URI → bitmap → upload Storage). |
| `data/ImageUtils.kt` | `extractBitmapDominantColors()`: estrae palette dominante da bitmap per colori dinamici UI. |
| `data/ITunesApiService.kt` | Retrofit client iTunes Search API — fallback per cover mancanti. |
| `data/MusicAffinity.kt` | Calcola punteggio di sintonia tra due utenti (generi condivisi, artisti, ascoltati). |
| `data/PulseRecorder.kt` | Registra audio mic (5s max) per la funzione Pulse — restituisce campioni waveform. |
| `data/PulseHaptics.kt` | Vibrazione sincrona alla voce durante la registrazione Pulse. |
| `data/PushNotificationSender.kt` | Invia notifiche FCM via HTTP v1 API (server-side key in BuildConfig). |
| `data/UpdateRepository.kt` | Controlla versione su GitHub Releases e scarica APK aggiornamento in-app. |

---

## 4. ViewModel

**`viewmodel/MusicViewModel.kt`** — unico ViewModel, `AndroidViewModel`.

- **State**: `MutableStateFlow<MusicUiState>` → esposto come `StateFlow` a `MainActivity`.
- **Responsabilità**: orchestra tutti i repository, gestisce polling Spotify, subscriptions Firestore, login/logout, chat, follow/unfollow, heart, pulse, aggiornamenti, ricerche.
- **Pattern chiave**:
  - Un solo `firebaseObserverJob` che osserva il feed real-time.
  - `spotifyPollingJob` ogni ~5s per brano corrente.
  - `liveStaleTickerJob` incrementa `liveTick` ogni 30s per forzare revalutazione TTL live.
  - Heartbeat ogni ~25s su Firestore per segnalare che si sta ancora ascoltando lo stesso brano.

### MusicUiState — campi principali

| Campo | Tipo | Uso |
|---|---|---|
| `isLoggedIn` | Boolean | Branch login/app in MainActivity |
| `currentUser` | User | Utente autenticato |
| `feedUsers` | List\<User\> | Feed live (chi sta ascoltando ora) |
| `activeProfileUser` | User? | Profilo aperto (non-null = pannello visibile) |
| `activeChatUser` | User? | Chat aperta |
| `conversations` | List\<Conversation\> | Lista conversazioni ChatList |
| `chatMessages` | Map\<userId, List\<ChatMessage\>\> | Messaggi per conversazione |
| `connectedServices` | Map\<String, Boolean\> | spotify / spotify_free / amazon_music |
| `heartedLiveKeys` | Set\<String\> | "broadcasterId:trackId" — dedup cuori live |
| `globalListenersCount` | Int | Chi ascolta lo stesso brano nel mondo |
| `applyCoverToFeed` | Boolean | Sfondo copertina nel feed |

---

## 5. Model

**`model/Models.kt`** — tutte le data class in un unico file.

| Classe | Campi chiave |
|---|---|
| `User` | id, name, username, avatarUrl, coverUrl, bio, currentTrack, sharedTracks, presenceState, followerIds, followingIds, liveHearts, stats |
| `Track` | id, title, artist, coverUrl, source ("spotify"/"amazon_music"), deviceType, genre, accentColorHex, progressMs, durationMs, sharedAt |
| `ChatMessage` | id, senderId, text, timestamp, isFromMe, attachedTrack, pulse, pulseAudioId |
| `Conversation` | id, recipientUser, lastMessageText, lastMessageAt, lastAttachedTrack |
| `FriendRequest` | id, fromUserId, toUserId, status, timestamp |
| `AppNotification` | id, type, fromUserId, trackId, timestamp, read |
| `UserStats` | listenedMs, listenedTracks, topListenedArtist |
| `ActivePulse` | audioPath, senderId, waveform |

---

## 6. Schermate

| File | Ruolo |
|---|---|
| `screens/LoginScreen.kt` | Login con Google. Badge "REAL-TIME SOCIAL VISUALIZER", glow verde, pulsante bianco. |
| `screens/MainFeedScreen.kt` | Schermata base. `HorizontalPager` (2 pagine: Live / Feed). TopBar con toggle, badge messaggi/notifiche. `FeedCarousel.kt` per la sezione feed settimanale. |
| `screens/LiveDetailScreen.kt` | Dettaglio live full-screen. Sfondo blur, onda audio, avatar con Pulse (hold-to-record), barra temporale, heart/reply, carosello ascoltatori, fit-scale responsive (<600dp). |
| `screens/ProfileScreen.kt` | Profilo. Sfondo atmosferico, identità + counter follow, carosello 3D brani condivisi, stats 2 pagine, FloatingLiveBar, sheet modifica/impostazioni/followers. |
| `screens/ChatListScreen.kt` | Lista conversazioni con search. Mostra anteprima ultimo messaggio + copertina brano allegato. |
| `screens/ChatScreen.kt` | Chat 1:1. Bolle con palette HSV derivata dalla cover, paginazione 30 msg, brani allegati, read receipt. |
| `screens/LiveStoryScreen.kt` | Story-style viewer della live di un utente (formato verticale story). |
| `screens/NotificationOnboardingScreen.kt` | Onboarding permesso `NotificationListenerService` (Spotify Free / Amazon Music). |
| `screens/FeedCarousel.kt` | Carosello orizzontale brani per la sezione Feed settimanale (componente estratto da MainFeed). |

---

## 7. Componenti riusabili (ui/components)

| File | Ruolo |
|---|---|
| `LiveEqualizerBadge.kt` | 3 barre animate — badge "in ascolto ora". |
| `PresenceDot.kt` | Pallino stato presenza (online/offline/live). |
| `TrackCard.kt` | Card brano nel feed settimanale (cover + titolo + genere + sintonia). |
| `TrackDetailDialog.kt` | Dialog dettaglio brano: copertina grande, stats, pulsante "Imposta come copertina". |
| `ListenOnPill.kt` | Pill "Ascolta su Spotify/Amazon" con ventaglio link. |
| `PanelHeader.kt` | Header uniforme per pannelli sovrapposti (titolo + freccia indietro). |
| `BottomNavBar.kt` | Bottom navigation bar comune (Feed / Chat / Profilo). |
| `NotificationsDialog.kt` | Dialog notifiche in-app (follow request, cuori, risposte). |
| `PeopleSearchDialog.kt` | Ricerca utenti globale. |
| `NowPlayingSheet.kt` | Bottom sheet mini-player brano corrente. |
| `GlobalListenersDialog.kt` | Dialog "chi ascolta questo brano nel mondo" con punteggio sintonia. |
| `PulseWave.kt` / `PulseReceiveDialog.kt` | UI per invio/ricezione messaggi vocali Pulse. |
| `StoryAvatarCircle.kt` | Avatar circolare con anello colorato (stile story). |
| `UpdateBanner.kt` | Banner aggiornamento in-app con progress download. |
| `ChangelogDialog.kt` | Dialog changelog post-aggiornamento (mostra `AppChangelog.LINES`). |

---

## 8. Tema

| File | Contenuto |
|---|---|
| `ui/theme/Color.kt` | Palette: `BlackPitch`, `BlackSurface`, `BlackCard`, `SpotifyGreen`, `PureWhite`, `SubtitleGray`, `Zinc400/500/900`, `CharcoalBorder`, `DarkGraphite` |
| `ui/theme/Type.kt` | TextStyle per titoli, body, label, caption |
| `ui/theme/Theme.kt` | `MyApplicationTheme` — MaterialTheme dark forzato |

---

## 9. Configurazione e build

| File | Contenuto |
|---|---|
| `app/build.gradle.kts` | `applicationId`: `com.aistudio.music.livefeed` · `minSdk 24` · `targetSdk 36` · firma con `debug.keystore` sia per release che debug · `versionCode` / `versionName` vanno aggiornati ad ogni commit |
| `.env` / `.env.example` | Secrets letti da Secrets Gradle Plugin (Spotify client ID, API key, ecc.) |
| `scripts/serviceAccountKey.json` | Credenziali Firebase Admin — **gitignored, non committare mai** |
| `google-services.json` | Config Firebase app — **gitignored** |
| `.github/workflows/build-apk.yml` | CI: `npm ci` → Gradle `assembleRelease` → GitHub Release `latest-release` |

---

## 10. Dipendenze chiave

| Libreria | Uso |
|---|---|
| Jetpack Compose BOM | UI dichiarativa |
| Firebase BOM (Firestore, Auth, Messaging) | Real-time DB, login, push |
| Coil | Caricamento immagini async con cache disk/memory |
| Retrofit + Moshi | HTTP client + JSON (Spotify Web API, iTunes) |
| OkHttp | HTTP raw (FCM send, download APK) |
| Room | (dipendenza presente, non ancora usata attivamente) |
| Credentials + GoogleID | Google Sign-In via Credential Manager |
| kotlinx.coroutines | Concorrenza, Flow, polling |
