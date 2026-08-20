package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BlackCard
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.BlackSurface
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.DarkGraphite
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SubtitleGray

data class FlowSpec(
    val title: String,
    val subtitle: String,
    val elements: String,
    val styleDetails: String,
    val visualInteractions: String,
    val midjourneyPrompt: String
)

@Composable
fun DesignSpecDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val specs = listOf(
        FlowSpec(
            title = "1. Flusso A: Feed Principale (Griglia 2D)",
            subtitle = "Schermata di atterraggio dopo il login con griglia bidimensionale snappata",
            elements = "Intestazione fissa con logo 'MUSIC', icone Search e Profilo. Riga orizzontale 'Stories a pallini' con bordo dinamico. Feed 2D verticale di utenti con scroll orizzontale snappato di card album senza pulsanti like.",
            styleDetails = "Dark mode radicale (#000000), testo bianco puro (#FFFFFF), accenti cromatici estratti dinamicamente dai brani.",
            visualInteractions = "Frecce di scorrimento orizzontale a scatto (snap) per ogni singola card brano, morphing al tap della story verso la vista full screen.",
            midjourneyPrompt = "/imagine prompt: Ultra minimalist mobile app UI screen design for \"MUSIC\", 2D music grid feed layout, pure pitch black #000000 background, bold white sans-serif typography, top live stories circular avatars with glowing dynamic neon borders, horizontal snappable high resolution vinyl album art cards with subtle author avatar in top left and clean bold track titles at bottom, semi-transparent modern floating action button (+), extreme negative space, no clutter, no like buttons, iOS 18 and Jetpack Compose native sleek aesthetic, 8k resolution, UI/UX Behance showcase --ar 9:16 --v 6.0"
        ),
        FlowSpec(
            title = "2. Flusso B: Condivisione e Ricerca Globale",
            subtitle = "Bottom Sheet con Now Playing da Spotify e ricerca catalogo istantanea",
            elements = "Pannello inferiore sollevato (Bottom Sheet). Sezione Spotlight 'Now Playing su Spotify' con visualizzatore audio animato e pulsante 'Condividi questa'. Barra di ricerca globale sotto con risultati filtrati.",
            styleDetails = "Gradiente dinamico scuro sfumato, bordo sottile, accento verde Spotify (#1DB954) e glow morbido.",
            visualInteractions = "Apertura fluida dal basso verso l'alto (spring animation), pubblicazione istantanea con feedback toast sul feed.",
            midjourneyPrompt = "/imagine prompt: Sleek modern dark mode mobile app UI bottom sheet modal for music sharing, titled \"MUSIC\", pure black #000000 canvas, spotlight card for \"Now Playing on Spotify\" with live equalizer bars, glowing dynamic neon album cover and huge bold white \"Share this\" action button, global search text bar underneath with filtered music results, hyper-minimalist typography, iOS glassmorphism details, clean Material 3, Behance trending UI --ar 9:16 --v 6.0"
        ),
        FlowSpec(
            title = "3. Flusso C: Stories Live (Full Screen)",
            subtitle = "Visualizzazione immersiva a tutto schermo di cosa un utente sta ascoltando ora",
            elements = "Copertina album a tutto schermo con sfocatura adattiva ai bordi, avatar circolare + nome utente + 'sta ascoltando ora' in alto, progress bar stile Instagram, titolo brano e artista extralarge in basso con barra di risposta rapida.",
            styleDetails = "Dark vignette gradients, tipografia display bold extra-large ad alto contrasto, accento dinamico dell'album.",
            visualInteractions = "Tap a destra per passare alla storia successiva, tap a sinistra per la precedente, swipe verso il basso per chiudere.",
            midjourneyPrompt = "/imagine prompt: Full screen immersive live music story mobile app UI for \"MUSIC\", atmospheric blurred album artwork background, top segmented Instagram-style progress bar, top left user profile picture with \"listening now\" status, massive ultra-bold display white typography for song title and artist at bottom, minimal pure text reply bar, dark mode radical black styling, cinematic lighting, modern UI design award --ar 9:16 --v 6.0"
        ),
        FlowSpec(
            title = "4. Flusso D: Profilo Utente e Statistiche",
            subtitle = "Profilo minimalista con 3 metriche chiave e griglia storica 3x3",
            elements = "Foto profilo circolare centrata, @username, badge Spotify connesso, sezione 3 statistiche numeriche ('Brani Condivisi', 'Top Artista', 'Generi / Minuti'), griglia storica 3x3 delle ultime 9 copertine.",
            styleDetails = "Layout centrato, numeri grandi bold, divisori grafite minimali, zero pulsanti superflui.",
            visualInteractions = "Tap sulla card 3x3 apre il pannello dettagli e messaggio.",
            midjourneyPrompt = "/imagine prompt: Minimalist user profile and music statistics mobile app UI for \"MUSIC\", pitch black #000000 background, centered circular profile avatar with dynamic glowing ring, large typography stats card with 3 clean metrics: Shared Tracks, Top Artist, Listening Minutes, 3x3 square grid of album covers below, high contrast white text, clean luxury dark aesthetic, Dribbble trending UI --ar 9:16 --v 6.0"
        ),
        FlowSpec(
            title = "5. Flusso E: Chat Integrata e Semplice",
            subtitle = "Flusso di testo puro per commentare la musica tra utenti",
            elements = "Sfondo nero radicale, bolle di chat minimaliste grafite/bianche, anteprima della card brano commentata, chip di risposta rapida, input di testo puro senza sticker o allegati pesanti.",
            styleDetails = "Sfondo #000000 puro, bolle #1C1C1E e #FFFFFF ad alta leggibilità, tipografia sans-serif precisa.",
            visualInteractions = "Invio istantaneo con animazione scorrevole e scroll automatico.",
            midjourneyPrompt = "/imagine prompt: Pure black radical dark mode mobile direct messaging chat UI for music lovers, \"MUSIC\" app, #000000 background, minimalist dark graphite and high contrast chat bubbles discussing music, small embedded album cover pill preview, clean text keyboard bar, extreme minimalist iOS aesthetic, no clutter, pristine typography --ar 9:16 --v 6.0"
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BlackPitch)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DESIGN SPEC & AI PROMPTS",
                            color = SpotifyGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Specifiche UI/UX & Prompt Midjourney / DALL-E",
                            color = PureWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkGraphite)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(specs) { spec ->
                        SpecCard(spec = spec, onCopy = { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("AI Prompt", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Prompt copiato negli appunti!", Toast.LENGTH_SHORT).show()
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecCard(
    spec: FlowSpec,
    onCopy: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BlackCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = spec.title,
                color = PureWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = spec.subtitle,
                color = SubtitleGray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Elementi UI: ${spec.elements}",
                color = PureWhite,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Stile Visivo: ${spec.styleDetails}",
                color = SubtitleGray,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Interazioni: ${spec.visualInteractions}",
                color = SubtitleGray,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Copyable Prompt Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkGraphite)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                    .clickable { onCopy(spec.midjourneyPrompt) }
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROMPT GENERATORE IMMAGINI (MIDJOURNEY / DALL-E 3)",
                            color = SpotifyGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copia Prompt",
                            tint = PureWhite,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = spec.midjourneyPrompt,
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
