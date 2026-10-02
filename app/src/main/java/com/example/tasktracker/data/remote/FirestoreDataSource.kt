package com.example.tasktracker.data.remote

import com.example.tasktracker.data.local.CompletionEntity
import com.example.tasktracker.data.local.TaskEntity
import com.example.tasktracker.data.local.TaskSelectionEntity
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

class FirestoreDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        /** Firestore allows max 500 writes per batch; stay safely below. */
        const val BATCH_LIMIT = 400
        private const val TIMEOUT_MS = 30_000L
    }

    private fun col(uid: String, name: String): CollectionReference =
        firestore.collection("users").document(uid).collection(name)

    /** commit().await() never fails while offline (it just waits) -> without a timeout the sync lock would hang forever. */
    private suspend fun WriteBatch.commitChecked() {
        val ok = withTimeoutOrNull(TIMEOUT_MS) { commit().await(); true }
        if (ok == null) throw IOException("Cloud server did not respond. Check your internet connection.")
    }

    /** Reads straight from the server (never the empty local cache of a fresh install). */
    private suspend fun Query.fetchFromServer() =
        withTimeoutOrNull(TIMEOUT_MS) { get(Source.SERVER).await() }
            ?: throw IOException("Could not reach the cloud server. Check your internet connection.")

    suspend fun pushTasks(uid: String, tasks: List<TaskEntity>) {
        val batch = firestore.batch()
        tasks.forEach { t -> batch.set(col(uid, "tasks").document(t.id), t.toMap()) }
        batch.commitChecked()
    }

    /** since > 0 -> only documents changed after that time (cheap incremental pull). */
    private fun changed(uid: String, name: String, since: Long): Query =
        if (since > 0) col(uid, name).whereGreaterThan("updatedAt", since) else col(uid, name)

    suspend fun fetchTasks(uid: String, since: Long = 0L): List<TaskEntity> =
        changed(uid, "tasks", since).fetchFromServer().documents
            .mapNotNull { d -> runCatching { d.toTask(d.id) }.getOrNull() }

    suspend fun pushCompletions(uid: String, list: List<CompletionEntity>) {
        val batch = firestore.batch()
        list.forEach { c -> batch.set(col(uid, "completions").document(c.id), c.toMap()) }
        batch.commitChecked()
    }

    suspend fun fetchCompletions(uid: String, since: Long = 0L): List<CompletionEntity> =
        changed(uid, "completions", since).fetchFromServer().documents
            .mapNotNull { d -> runCatching { d.toCompletion(d.id) }.getOrNull() }

    suspend fun pushSelections(uid: String, list: List<TaskSelectionEntity>) {
        val batch = firestore.batch()
        list.forEach { s -> batch.set(col(uid, "selections").document(s.id), s.toMap()) }
        batch.commitChecked()
    }

    suspend fun fetchSelections(uid: String, since: Long = 0L): List<TaskSelectionEntity> =
        changed(uid, "selections", since).fetchFromServer().documents
            .mapNotNull { d -> runCatching { d.toSelection(d.id) }.getOrNull() }

    /** App settings (profile name, contact, theme) - one document per user. */
    suspend fun pushSettings(uid: String, data: Map<String, Any>) {
        val ok = withTimeoutOrNull(TIMEOUT_MS) {
            col(uid, "settings").document("app").set(data, SetOptions.merge()).await(); true
        }
        if (ok == null) throw IOException("Cloud server did not respond. Check your internet connection.")
    }

    suspend fun fetchSettings(uid: String): Map<String, Any>? {
        val snap = withTimeoutOrNull(TIMEOUT_MS) {
            col(uid, "settings").document("app").get(Source.SERVER).await()
        } ?: throw IOException("Could not reach the cloud server. Check your internet connection.")
        return if (snap.exists()) snap.data else null
    }

    /** Profile picture: small JPEG (base64) in its own document, so no Firebase Storage setup is needed. */
    suspend fun pushPhoto(uid: String, base64: String, stamp: Long) {
        val batch = firestore.batch()
        batch.set(col(uid, "settings").document("photo"), mapOf("data" to base64, "updatedAt" to stamp))
        batch.set(col(uid, "settings").document("app"), mapOf("photoUpdatedAt" to stamp), SetOptions.merge())
        batch.commitChecked()
    }

    suspend fun fetchPhoto(uid: String): String? {
        val snap = withTimeoutOrNull(TIMEOUT_MS) {
            col(uid, "settings").document("photo").get(Source.SERVER).await()
        } ?: throw IOException("Could not reach the cloud server. Check your internet connection.")
        return if (snap.exists()) snap.getString("data") else null
    }
}
