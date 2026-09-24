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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
 * with a clean, scrollable layout and zero artificial shadows.
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

    val lastUpdated = "September 2026"

    val sections: List<LegalSection> = when (type) {
        LegalDocumentType.PRIVACY_POLICY -> privacyPolicySections
        LegalDocumentType.TERMS_OF_SERVICE -> termsOfServiceSections
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(44.dp)
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
                        modifier = Modifier.size(24.dp)
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
                    text = "Effective: $lastUpdated",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                sections.forEachIndexed { index, section ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
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
        content = "Scanorea operates under a strict privacy-first and local-first architecture. We believe your documents, scans, and personal records belong solely to you. This Privacy Policy outlines how your information is handled exclusively on your device without third-party surveillance or external data mining."
    ),
    LegalSection(
        title = "2. Zero Cloud Collection & 100% On-Device Processing",
        content = "All image processing operations (including camera capture, edge detection, cropping, perspective distortion adjustments, color filter matrix computations, page reordering, and PDF file rendering) are executed entirely within your device's local memory (RAM and internal app storage). Scanorea does not operate external document processing servers, does not upload your files to the cloud, and does not transmit telemetry containing your scanned documents."
    ),
    LegalSection(
        title = "3. Device Permissions & Purpose of Use",
        content = "Scanorea requests only the minimal set of operating system permissions required for direct document conversion:\n\n• Camera: Utilized solely when you intentionally trigger live capture. No background recording, passive camera activation, or image archiving occurs outside of your active scanning session.\n\n• Storage / Scoped Storage Access: Used strictly to allow you to import images from your photo gallery and export completed PDF files to your preferred storage location via the Android Storage Access Framework (SAF)."
    ),
    LegalSection(
        title = "4. Metadata, Folders & Local Preferences",
        content = "Any organization choices you make (such as folder assignments, filename templates, document tags, favorites, and paper size presets) are stored strictly on your device using encrypted Android private storage. This metadata is never synchronized with external analytics providers or sold to third parties."
    ),
    LegalSection(
        title = "5. Data Retention & User Control",
        content = "You maintain complete sovereignty over your data at all times. Deleting a document within Scanorea or clearing the application data through Android system settings permanently removes the corresponding files and metadata from your device. No residual copies remain on external servers because none were ever uploaded."
    ),
    LegalSection(
        title = "6. Security Architecture",
        content = "Because Scanorea processes everything locally, your documents are shielded by Android's application sandboxing architecture and hardware-backed device encryption. We advise users to maintain strong device lock credentials to preserve file security on their physical hardware."
    ),
    LegalSection(
        title = "7. Policy Updates & Inquiries",
        content = "We may periodically revise this policy to reflect newly introduced local features. Any modifications will be posted directly within the application settings for full transparency."
    )
)

private val termsOfServiceSections = listOf(
    LegalSection(
        title = "1. Agreement to Terms",
        content = "By downloading, installing, accessing, or utilizing Scanorea, you agree to be bound by these Terms of Service. If you do not agree to these terms in their entirety, you must discontinue use and uninstall the application immediately."
    ),
    LegalSection(
        title = "2. License Grant & Permitted Scope",
        content = "Scanorea grants you a personal, revocable, non-exclusive, non-transferable, and royalty-free license to use the software for personal, academic, and business document management purposes in compliance with these terms and applicable laws."
    ),
    LegalSection(
        title = "3. Document Ownership & Intellectual Property",
        content = "You retain 100% ownership and copyright of all original photographs, images, scanned texts, and generated PDF files processed through the application. Scanorea asserts no ownership, proprietary claim, or licensing rights over your user-generated content."
    ),
    LegalSection(
        title = "4. Prohibited Uses",
        content = "You agree not to use Scanorea to:\n\n• Create forged, fraudulent, counterfeit, or deceptive governmental or financial documentation.\n• Infringe upon third-party intellectual property, trademarks, or copyrights.\n• Decompile, reverse engineer, disassemble, or attempt to derive the application source code outside of applicable open-source permissions.\n• Interfere with device security mechanisms or circumvent Android sandboxing policies."
    ),
    LegalSection(
        title = "5. Disclaimer of Warranties",
        content = "Scanorea is provided on an 'AS IS' and 'AS AVAILABLE' basis without warranties of any kind, whether express, statutory, or implied, including but not limited to merchantability, fitness for a particular purpose, and non-infringement. While we implement high-standard algorithms for PDF generation, you acknowledge that critical records should be independently verified and securely backed up."
    ),
    LegalSection(
        title = "6. Limitation of Liability",
        content = "To the maximum extent permitted by applicable law, in no event shall the developers or contributors of Scanorea be liable for any direct, indirect, incidental, special, consequential, or punitive damages resulting from the use or inability to use the application, data loss, hardware failure, or business interruption."
    ),
    LegalSection(
        title = "7. Modifications & Termination",
        content = "We reserve the right to modify, suspend, or discontinue any feature within the application at any time without prior liability. Your continued use of the application following updates constitutes acceptance of the modified terms."
    ),
    LegalSection(
        title = "8. Governing Law & Severability",
        content = "These Terms are governed by and construed in accordance with the laws of the jurisdiction in which the developer resides and applicable international consumer protection regulations, without giving effect to conflict of laws principles."
    )
)
