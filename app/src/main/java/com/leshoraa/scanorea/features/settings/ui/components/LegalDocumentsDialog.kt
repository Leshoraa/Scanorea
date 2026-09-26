package com.leshoraa.scanorea.features.settings.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val DIALOG_SHAPE_CORNER = 24.dp
private val ICON_CONTAINER_CORNER = 12.dp
private val ICON_CONTAINER_SIZE = 44.dp
private val ICON_SIZE = 24.dp
private val CONTENT_MAX_HEIGHT = 420.dp
private val SECTION_SPACING = 14.dp
private val TITLE_CONTENT_SPACING = 6.dp
private val DIVIDER_THICKNESS = 0.5.dp
private const val DIVIDER_ALPHA = 0.35f
private const val DOCUMENT_VERSION_LABEL = "Version 1.0.0 • Effective: September 2026"

/**
 * Type of legal document to be rendered.
 */
enum class LegalDocumentType(val title: String) {
    PRIVACY_POLICY("Privacy Policy"),
    TERMS_OF_SERVICE("Terms of Service")
}

private data class LegalSection(
    val title: String,
    val content: String
)

/**
 * Material 3 dialog presenting comprehensive legal documentation (Privacy Policy or Terms of Service)
 * with a clean, scrollable layout, clear heading semantics, and zero artificial shadows.
 */
@Composable
fun LegalDocumentDialog(
    type: LegalDocumentType,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (type) {
        LegalDocumentType.PRIVACY_POLICY -> Icons.Outlined.Policy
        LegalDocumentType.TERMS_OF_SERVICE -> Icons.Outlined.Gavel
    }

    val sections: List<LegalSection> = when (type) {
        LegalDocumentType.PRIVACY_POLICY -> privacyPolicySections
        LegalDocumentType.TERMS_OF_SERVICE -> termsOfServiceSections
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(DIALOG_SHAPE_CORNER),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Surface(
                shape = RoundedCornerShape(ICON_CONTAINER_CORNER),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(ICON_CONTAINER_SIZE)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(ICON_SIZE)
                    )
                }
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = type.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DOCUMENT_VERSION_LABEL,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = CONTENT_MAX_HEIGHT)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                sections.forEachIndexed { index, section ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(SECTION_SPACING))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA),
                            thickness = DIVIDER_THICKNESS
                        )
                        Spacer(modifier = Modifier.height(SECTION_SPACING))
                    }

                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(modifier = Modifier.height(TITLE_CONTENT_SPACING))
                    Text(
                        text = section.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        modifier = modifier
    )
}

private val privacyPolicySections = listOf(
    LegalSection(
        title = "1. Introduction & Core Principle",
        content = "Scanorea operates under a strict privacy-by-design and local-first architecture. We believe your documents, scans, and personal records belong solely to you. This Privacy Policy outlines how your data is handled exclusively on your device without third-party surveillance, telemetry, or external data mining."
    ),
    LegalSection(
        title = "2. Zero Cloud Collection & 100% On-Device Processing",
        content = "All image processing operations—including camera capture, edge detection, perspective distortion adjustments, cropping, color filter matrix computations, page reordering, and PDF file rendering—are executed entirely within your device's local memory and internal application storage. Scanorea operates no external document processing servers, uploads zero files to the cloud, and never transmits your documents."
    ),
    LegalSection(
        title = "3. Data We Do NOT Collect",
        content = "Scanorea strictly adheres to data minimization. We do NOT collect, harvest, monitor, or transmit any of the following:\n\n" +
                "• Personal Identifiers: No names, email addresses, phone numbers, or account credentials.\n" +
                "• Device Identifiers: No advertising IDs (GAID, IDFA), IMEI, MAC address, or hardware serial numbers.\n" +
                "• Location Data: No GPS coordinates, Wi-Fi triangulation, or network-based geo-tracking.\n" +
                "• Contacts & Calendar: No access to your address book, phone records, or personal appointments.\n" +
                "• Audio & Biometrics: No background recording, microphone access, or facial recognition biometric data.\n" +
                "• Browsing & App Activity: No tracking of websites visited, external apps, or usage telemetry."
    ),
    LegalSection(
        title = "4. Zero Third-Party Trackers, Analytics & Advertising",
        content = "Scanorea contains ZERO third-party analytics SDKs, advertising frameworks, or crash monitoring trackers (including no Google Analytics, no Firebase Analytics, no Facebook SDK, and no AdMob). Your in-app activity is completely confidential, unmonitored, and unmonetized."
    ),
    LegalSection(
        title = "5. Device Permissions & Purpose of Use",
        content = "Scanorea requests only the minimal set of operating system permissions strictly necessary for core document scanning functionality:\n\n" +
                "• Camera (android.permission.CAMERA): Utilized solely when you intentionally trigger live capture. No background recording, silent activation, or image archiving occurs outside of your active scanning session.\n\n" +
                "• Storage / Scoped Storage Access: Used strictly via Android's Storage Access Framework (SAF) to enable importing images from your photo gallery and exporting finished PDF files to your chosen local storage locations."
    ),
    LegalSection(
        title = "6. Data Retention, Storage & User Control",
        content = "All scanned documents, drafts, folder hierarchies, and preferences are stored exclusively within Android's private app-specific storage. You maintain total sovereignty over your data at all times:\n\n" +
                "• Export: You can export your documents as standard PDF files at any time via SAF.\n" +
                "• Immediate Deletion: Deleting a document or folder within Scanorea permanently purges it from your local storage immediately.\n" +
                "• Complete Wipe: Clearing the application data in Android settings or uninstalling the app permanently deletes all associated documents and metadata from the device. Zero residual copies remain on remote servers because none were ever created."
    ),
    LegalSection(
        title = "7. Children's Privacy (COPPA & GDPR-K Compliance)",
        content = "Because Scanorea does not collect, transmit, or store any personal data whatsoever, it is inherently safe for users of all ages, fully complying with the Children's Online Privacy Protection Act (COPPA) and Article 8 of the GDPR."
    ),
    LegalSection(
        title = "8. Third-Party Sharing via Android System Share Sheet",
        content = "When you explicitly choose to export or share a generated PDF document using Android's system share sheet (e.g., via email, instant messaging, or personal cloud storage), the transfer occurs directly through the external application you selected. Such transfers are governed solely by that third party's privacy policy and terms."
    ),
    LegalSection(
        title = "9. Policy Updates & Inquiries",
        content = "We may update this policy if new on-device features warrant clarifications. All revisions will be published directly within the application's Legal & Privacy settings for full transparency.\n\n" +
                "For inquiries, support, or privacy verification, contact:\n" +
                "Email: support@scanorea.app"
    )
)

private val termsOfServiceSections = listOf(
    LegalSection(
        title = "1. Agreement to Terms",
        content = "By downloading, installing, accessing, or utilizing Scanorea, you agree to be bound by these Terms of Service. If you do not agree to these terms in their entirety, you must discontinue use and uninstall the application immediately."
    ),
    LegalSection(
        title = "2. Accountless Architecture",
        content = "Scanorea requires no user accounts, passwords, email registrations, or cloud profiles. You are not required to provide any personal credentials to access any feature of the application."
    ),
    LegalSection(
        title = "3. License Grant & Permitted Scope",
        content = "Scanorea grants you a personal, revocable, non-exclusive, non-transferable, and royalty-free license to use the software for personal, academic, and business document management purposes in compliance with these terms and applicable laws."
    ),
    LegalSection(
        title = "4. Document Ownership & Intellectual Property",
        content = "You retain 100% unconditional ownership and copyright of all original photographs, images, scanned texts, and generated PDF files processed through the application. Scanorea asserts no ownership, proprietary claim, or licensing rights over your user-generated content."
    ),
    LegalSection(
        title = "5. Prohibited Uses",
        content = "You agree not to use Scanorea to:\n\n" +
                "• Create forged, fraudulent, counterfeit, or deceptive governmental or financial documentation.\n" +
                "• Infringe upon third-party intellectual property, trademarks, or copyrights.\n" +
                "• Decompile, reverse engineer, disassemble, or attempt to derive the application source code outside of applicable open-source permissions.\n" +
                "• Interfere with device security mechanisms or circumvent Android sandboxing policies."
    ),
    LegalSection(
        title = "6. Third-Party Services Disclaimer",
        content = "Scanorea functions fully offline and does not control third-party services (such as external cloud drives, email clients, or messaging apps) that you may interact with via Android's system share sheet. We assume no responsibility or liability for third-party service availability, terms, or privacy practices."
    ),
    LegalSection(
        title = "7. Disclaimer of Warranties",
        content = "Scanorea is provided on an 'AS IS' and 'AS AVAILABLE' basis without warranties of any kind, whether express, statutory, or implied, including but not limited to merchantability, fitness for a particular purpose, and non-infringement. While we implement high-standard algorithms for PDF generation, you acknowledge that critical records should be independently verified and securely backed up."
    ),
    LegalSection(
        title = "8. Limitation of Liability",
        content = "To the maximum extent permitted by applicable law, in no event shall the developers or contributors of Scanorea be liable for any direct, indirect, incidental, special, consequential, or punitive damages resulting from the use or inability to use the application, data loss, hardware failure, or business interruption."
    ),
    LegalSection(
        title = "9. Modifications to Terms & Contact",
        content = "We reserve the right to modify these terms at any time. Continued use of the application following updates constitutes acceptance of the modified terms.\n\n" +
                "For questions or legal notices, contact:\n" +
                "Email: support@scanorea.app"
    )
)
