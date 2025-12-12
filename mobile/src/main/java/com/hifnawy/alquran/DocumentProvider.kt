package com.hifnawy.alquran

import android.content.Context
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.hifnawy.alquran.DocumentProvider.Companion.AUTHORITY
import com.hifnawy.alquran.DocumentProvider.Companion.DEFAULT_DOCUMENT_PROJECTION
import com.hifnawy.alquran.DocumentProvider.Companion.DEFAULT_ROOT_PROJECTION
import com.hifnawy.alquran.DocumentProvider.Companion.ROOT_ID
import com.hifnawy.alquran.shared.QuranApplication
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * A custom [DocumentsProvider] that exposes the application's internal file storage
 * to the Android Storage Access Framework (`SAF`).
 *
 * This provider allows other applications, with the user's permission, to interact
 * with files stored within this app's private data directory (specifically, the directory
 * returned by [context.filesDir][Context.filesDir].[parentFile][File.parentFile]). This is
 * useful for `backup` / `restore` functionality, or for allowing users to manage app data
 * using their preferred file manager.
 *
 * ## Key Responsibilities:
 *
 * - **Root Declaration:** Defines a single "root" which represents the base directory
 *   of the application's files. This root is what users see when they browse this
 *   app's storage via the `SAF` file picker.
 *
 * - **Document ID Management:** Translates between the file system's [File] paths and the
 *   [documentId] strings required by the `SAF`. The [documentId] is a stable identifier that
 *   the provider uses to refer to a specific file or directory. Here, it's constructed by
 *   prefixing the relative file path with [ROOT_ID].
 *
 * - **Querying:** Implements [queryRoots], [queryDocument], and [queryChildDocuments]
 *   to allow other apps to list the available storage roots, get metadata for a
 *   specific `file` / `directory`, and list the `contents` of a `directory`.
 *
 * - **File Operations:** Implements all core file manipulation methods required by the framework:
 *    - `openDocument`: Provides read/write access to a file's content.
 *    - `createDocument`: Creates new files or directories.
 *    - `deleteDocument` / `removeDocument`: Deletes files or directories.
 *    - `renameDocument`: Renames a file or directory.
 *
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see queryRoots
 * @see queryDocument
 * @see queryChildDocuments
 * @see openDocument
 * @see createDocument
 * @see deleteDocument
 * @see removeDocument
 * @see renameDocument
 */
class DocumentProvider : DocumentsProvider() {

    /**
     * Contains constants and default projections used by the [DocumentProvider].
     * This object centralizes static configuration values, making them easily accessible
     * and manageable within the provider class.
     *
     * @property AUTHORITY [String] The authority for this document provider. It's a unique identifier
     * for the provider within the Android system, constructed from the application's ID and a suffix.
     *
     * @property ROOT_ID [String] A constant [String] that identifies the root directory of the provider.
     * This is used to build and parse document IDs.
     *
     * @author AbdElMoniem ElHifnawy
     */
    companion object {

        /**
         * The unique authority for this [DocumentsProvider].
         *
         * This authority is registered in the `AndroidManifest.xml` and is used by the
         * Android system to identify and route requests to this provider. It must be unique
         * across all installed applications on the device.
         *
         * It is constructed by appending `.file_provider` to the application's unique ID,
         * ensuring its uniqueness.
         */
        const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.file_provider"

        /**
         * A constant string that identifies the root directory for this provider.
         *
         * This ID is used by the `SAF` to uniquely identify the top-level directory
         * exposed by this provider. It serves as a prefix for all [documentId]s,
         * creating a namespace for files and directories.
         */
        const val ROOT_ID = "root"

        /**
         * A default projection for querying roots.
         *
         * This array specifies the columns of data that should be returned by default when a client
         * queries for the available storage roots (in [queryRoots]). Using a default
         * projection ensures that the provider returns all the necessary metadata for the
         * system UI to correctly display the root.
         *
         * The columns included are:
         * - [DocumentsContract.Root.COLUMN_ROOT_ID]: A unique ID for the root.
         * - [DocumentsContract.Root.COLUMN_MIME_TYPES]: The MIME types supported by this root.
         * - [DocumentsContract.Root.COLUMN_FLAGS]: Flags indicating the capabilities of the root.
         * - [DocumentsContract.Root.COLUMN_ICON]: A drawable resource for the root's icon.
         * - [DocumentsContract.Root.COLUMN_TITLE]: The user-visible title for the root.
         * - [DocumentsContract.Root.COLUMN_SUMMARY]: A summary or description of the root's content.
         * - [DocumentsContract.Root.COLUMN_DOCUMENT_ID]: The document ID of the root directory itself.
         * - [DocumentsContract.Root.COLUMN_AVAILABLE_BYTES]: The available storage space within the root.
         *
         * @see queryRoots
         * @see DocumentsContract.Root
         */
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
                DocumentsContract.Root.COLUMN_ROOT_ID,
                DocumentsContract.Root.COLUMN_DOCUMENT_ID,
                DocumentsContract.Root.COLUMN_TITLE,
                DocumentsContract.Root.COLUMN_SUMMARY,
                DocumentsContract.Root.COLUMN_ICON,
                DocumentsContract.Root.COLUMN_MIME_TYPES,
                DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
                DocumentsContract.Root.COLUMN_FLAGS
        )

        /**
         * Default column projection for a document query.
         *
         * This projection is used by default in [queryDocument] and [queryChildDocuments]
         * if `null` is passed for the `projection` parameter. Using a default
         * projection ensures that the provider returns all the necessary metadata for the
         * system UI to correctly display the document.
         *
         * The columns included are:
         * - [DocumentsContract.Document.COLUMN_DOCUMENT_ID]: A unique ID for a document.
         * - [DocumentsContract.Document.COLUMN_MIME_TYPE]: The MIME type representing a document.
         * - [DocumentsContract.Document.COLUMN_DISPLAY_NAME]: The Display name of a document.
         * - [DocumentsContract.Document.COLUMN_LAST_MODIFIED]: A Timestamp of when a document was last modified.
         * - [DocumentsContract.Document.COLUMN_FLAGS]: Flags indicating the capabilities of the document.
         * - [DocumentsContract.Document.COLUMN_SIZE]: The size of a document.
         *
         * @see queryDocument
         * @see queryChildDocuments
         * @see DocumentsContract.Document
         */
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_FLAGS,
                DocumentsContract.Document.COLUMN_SIZE
        )
    }

    /**
     * The root directory that this provider exposes.
     *
     * It points to the parent of the application's private `files` directory
     * (`/data/data/APPLICATION_ID/`). This allows access to not just
     * the `files` directory but also other sibling directories like `databases` or `shared_prefs`,
     * which is useful for comprehensive backup and restore operations.
     */
    private val rootDir get() = QuranApplication.applicationContext.filesDir.parentFile

    /**
     * The application's internal file storage directory.
     *
     * This directory is conventionally used for storing persistent files that the
     * application creates. It is located at the path returned by [Context.filesDir].
     */
    private val filesDir get() = QuranApplication.applicationContext.filesDir

    /**
     * The base directory that the provider exposes.
     *
     * This is the top-level directory that users will see when browsing this provider.
     * It attempts to use the application's [rootDir] directory, which contains `files`,
     * `cache`, `databases`, etc. If that is unavailable, it falls back to the [filesDir]
     * directory itself.
     */
    private val baseDirectory get() = rootDir ?: filesDir

    /**
     * Computes the `SAF`-compatible [documentId] for a given [File].
     *
     * The [documentId] is a stable, unique string that the [DocumentsProvider] uses to
     * identify a file or directory. It's constructed by concatenating the [ROOT_ID]
     * with the file's path relative to the [baseDirectory].
     *
     * @receiver [File] A [File] to compute the [documentId] for.
     */
    private val File.documentId get() = "$ROOT_ID/${toRelativeString(baseDirectory)}"

    /**
     * Converts a [documentId] string into a [File] object.
     *
     * This computed property acts as a translator between the Storage Access Framework's
     * document IDs and the underlying file system paths. It validates that the given
     * [documentId] string starts with the [ROOT_ID], then resolves the remaining
     * part of the string as a relative path from the [baseDirectory].
     *
     * @receiver A [String] representing the [documentId].
     *
     * @throws FileNotFoundException if the [documentId] does not start with the [ROOT_ID],
     *   or if the resulting file path does not exist on the file system.
     */
    private val String.asFile
        get() = when {
            startsWith(ROOT_ID) -> {
                val file = baseDirectory.resolve(drop(ROOT_ID.length + 1))
                if (!file.exists()) throw FileNotFoundException("${file.absolutePath} '${this@asFile}' not found")

                file
            }

            else                -> throw FileNotFoundException("'${this@asFile}' is not in any known root")
        }

    /**
     * Determines the `MIME` type for a given [File].
     *
     * This computed property checks if the file is a `directory` or a `regular file`:
     * - If it's a `directory`, it returns [DocumentsContract.Document.MIME_TYPE_DIR].
     * - If it's a `file`, it attempts to resolve the `MIME` type from the file's extension
     *   using [MimeTypeMap].
     * - If the `MIME` type cannot be determined from the extension, it falls back to the
     *   generic binary stream type, `application/octet-stream`.
     *
     * @receiver [File] A [File] to determine the `MIME` type for.
     */
    private val File.mimeType
        get() = when {
            isDirectory -> DocumentsContract.Document.MIME_TYPE_DIR
            else        -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octect-stream"
        }

    /**
     * Called by the system when the provider is first created.
     *
     * This method is part of the Android component lifecycle. For a [DocumentsProvider],
     * its primary role is to perform minimal initialization. Heavy-lifting, such as
     * file system access, should be deferred until a specific query or file operation
     * is requested.
     *
     * In this implementation, we simply return `true` to indicate that the provider
     * was successfully loaded and is ready to handle requests. No complex initialization
     * is needed at this stage.
     *
     * @return [Boolean] `true` to indicate successful initialization of the provider.
     */
    override fun onCreate() = true

    /**
     * Queries the available storage roots that this provider offers.
     *
     * The Android `SAF` calls this method to discover the top-level directories (roots)
     * that the user can browse. This implementation defines a single root that corresponds
     * to the application's internal storage directory.
     *
     * The method constructs a [MatrixCursor] and populates it with a single row
     * representing this root. The columns of the cursor contain metadata about the root,
     * such as its `title`, `icon`, `available space`, and `capabilities` (whether new
     * files can be created within it).
     *
     * @param projection [Array< out String >?][Array] An optional array of column names to return. If `null`, the
     *   [DEFAULT_ROOT_PROJECTION] is used, which contains all standard root columns.
     *
     * @return [MatrixCursor] A [MatrixCursor] containing metadata for the single root exposed by this provider.
     *   Each row in the cursor represents one root.
     *
     * @see DEFAULT_ROOT_PROJECTION
     * @see DocumentsContract.Root
     */
    override fun queryRoots(projection: Array<out String>?) = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION).apply {
        newRow().apply {
            val columnFlags = DocumentsContract.Root.FLAG_SUPPORTS_CREATE or DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD

            add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID)
            add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, baseDirectory.documentId)
            add(DocumentsContract.Root.COLUMN_TITLE, context!!.getString(R.string.app_name))
            add(DocumentsContract.Root.COLUMN_SUMMARY, null)
            add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher)
            add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
            add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, baseDirectory.freeSpace)
            add(DocumentsContract.Root.COLUMN_FLAGS, columnFlags)
        }
    }

    /**
     * Retrieves metadata for a specific document (`file` or `directory`).
     *
     * This method is called by the `SAF` to get information about a single document,
     * such as its `name`, size, `MIME` type, and `supported actions`. The document
     * is identified by its unique [documentId].
     *
     * The implementation creates a [MatrixCursor] with the requested [projection] (or
     * the [DEFAULT_DOCUMENT_PROJECTION] if `null`) and then delegates to the
     * [includeFile] helper method to populate the cursor with the document's details.
     *
     * @param documentId [String] The unique ID of the document to query. This ID was previously
     *   returned by [queryRoots] or [queryChildDocuments].
     * @param projection [Array< out String >?][Array] A list of columns to return, or `null` to return all default
     *   columns defined in [DEFAULT_DOCUMENT_PROJECTION].
     *
     * @return [MatrixCursor] A [MatrixCursor] containing a single row with the requested document metadata.
     *
     * @throws FileNotFoundException If the [documentId] is invalid or the corresponding
     *   file does not exist.
     *
     * @see includeFile
     */
    override fun queryDocument(documentId: String, projection: Array<out String>?) = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).run {
        includeFile(this@run, documentId, null)
    }

    /**
     * Returns a [MatrixCursor] containing metadata for the immediate children
     * of a given parent directory.
     *
     * This method is called by the `SAF` when a user navigates into a directory
     * within this provider. It converts the [parentDocumentId] to a [File] object
     * representing the directory, lists its contents, and then populates a
     * [MatrixCursor] with details for each child file and subdirectory.
     *
     * The `sortOrder` parameter is currently ignored in this implementation; files
     * are returned in the default order provided by the file system.
     *
     * @param parentDocumentId [String] The [documentId] of the directory whose children are to be queried.
     * @param projection [Array< out String >?][Array] A list of columns to return. If `null`, the
     *   [DEFAULT_DOCUMENT_PROJECTION] is used.
     * @param sortOrder [String?][String] The sorting order for the results. This is currently ignored.
     *
     * @return [MatrixCursor] A [MatrixCursor] containing the requested columns for each child document.
     *
     * @throws FileNotFoundException if the [parentDocumentId] does not correspond to a valid directory.
     *
     * @see includeFile
     */
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?) = parentDocumentId.asFile.run {
        var cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)

        listFiles()?.forEach { file -> cursor = includeFile(cursor, null, file) }

        cursor
    }

    /**
     * Checks if a given document is a descendant of a parent document.
     *
     * This method is an optional optimization for the `SAF`.
     * It provides a quick way to determine the parent-child relationship between two
     * documents without needing to access the file system. In this implementation, a
     * document is considered a child if its [documentId] string starts with the
     * [parentDocumentId] string, which is true due to the hierarchical way document IDs
     * are constructed from file paths.
     *
     * @param parentDocumentId [String] The [documentId] of the potential parent.
     * @param documentId [String] The [documentId] of the potential child.
     *
     * @return [Boolean] `true` if [documentId] is a descendant of [parentDocumentId], `false` otherwise.
     */
    override fun isChildDocument(parentDocumentId: String, documentId: String) = documentId.startsWith(parentDocumentId)

    /**
     * Creates a new document (either a `file` or a `directory`) within a given parent directory.
     *
     * This method is called by the `SAF` when a client application requests to create a new
     * item in a directory managed by this provider. It handles the creation of both regular
     * files and subdirectories based on the provided [mimeType].
     *
     * To avoid overwriting existing files, it uses the [resolveUnique] helper
     * to generate a unique file name if a file with the given [displayName] already exists.
     *
     * @param parentDocumentId [String] The [documentId] of the `directory` in which to create the new document.
     * @param mimeType [String] The `MIME` type for the new document. If this is
     *   [DocumentsContract.Document.MIME_TYPE_DIR], a new `directory` will be created.
     *   Otherwise, a new `file` will be created.
     * @param displayName [String] The user-visible name for the new document.
     *
     * @return [String] The [documentId] of the newly created document.
     *
     * @throws FileNotFoundException if the parent directory cannot be found, or if the `file` / `directory`
     *   creation fails due to an [IOException].
     *
     * @see resolveUnique
     */
    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String) = try {
        val newFile = parentDocumentId.asFile.resolveUnique(displayName)

        when (mimeType) {
            DocumentsContract.Document.MIME_TYPE_DIR -> if (!newFile.mkdir()) throw IOException("Failed to create directory '${newFile.path}'")
            else                                     -> if (!newFile.createNewFile()) throw IOException("Failed to create file '${newFile.path}'")
        }

        newFile.documentId
    } catch (ex: IOException) {
        throw FileNotFoundException(ex.message)
    }

    /**
     * Opens a document for reading or writing.
     *
     * This method is called by the `SAF` when a client application wants to access the
     * content of a file. It translates the `SAF` [documentId] into a local [File] object
     * and then uses [ParcelFileDescriptor.open] to provide access to it.
     *
     * The access [mode] (`r` for `read`, `w` for `write`, `rw` for `read` / `write`) is
     * parsed from the string argument into the appropriate file mode flags required by the
     * underlying file system.
     *
     * The [CancellationSignal] allows the calling application to cancel the open operation
     * if it's taking too long, for example. This implementation passes the signal directly
     * to the underlying open call, but does not actively monitor it itself.
     *
     * @param documentId [String] The [documentId] of the document to open.
     * @param mode [String] The access mode, `r` for `read`, `w` for `write`, `rw` for `read` / `write`.
     * @param signal [CancellationSignal?][CancellationSignal] A signal to observe for cancellation requests.
     *
     * @return [ParcelFileDescriptor] A [ParcelFileDescriptor] for the requested document.
     *
     * @throws FileNotFoundException if the [documentId] is invalid, the file does not exist,
     *   or the requested access [mode] is not permitted.
     */
    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?) =
            ParcelFileDescriptor.open(documentId.asFile, ParcelFileDescriptor.parseMode(mode)) as ParcelFileDescriptor

    /**
     * Renames a specified document (`file` or `directory`).
     *
     * This method is triggered when a client application attempts to rename an item
     * managed by this provider. It locates the original `file` / `directory` using the
     * [documentId], determines its parent directory, and then renames it to the
     * new [displayName] within that same directory.
     *
     * @param documentId [String] The [documentId] of the document to rename.
     * @param displayName [String] The new, user-visible name for the document.
     *
     * @return [String] The [documentId] of the renamed document. Note that because the file
     *   path changes, the [documentId] will also change.
     *
     * @throws FileNotFoundException If the document specified by [documentId] cannot be
     *   found, does not have a parent directory, or if the rename operation fails
     *   (due to file system permissions or if a file with the new name already exists).
     */
    override fun renameDocument(documentId: String, displayName: String) = try {
        val sourceFile = documentId.asFile
        val sourceParentFile = sourceFile.parentFile ?: throw FileNotFoundException("Couldn't rename document '$documentId' as it has no parent")
        val destFile = sourceParentFile.resolve(displayName)

        if (!sourceFile.renameTo(destFile)) throw FileNotFoundException("Couldn't rename document from '${sourceFile.name}' to '${destFile.name}'")

        destFile.documentId
    } catch (ex: Exception) {
        throw FileNotFoundException(ex.message)
    }

    /**
     * Copies a document from one location to another within the provider.
     *
     * This method is called by the `SAF` when a client requests to copy a file. It
     * first resolves the [sourceDocumentId] and [targetParentDocumentId] using [asFile].
     * It then creates a new file in the target directory, ensuring there
     * are no name conflicts by using [resolveUnique]. Finally, it streams the
     * content from the source file to the new file.
     *
     * @param sourceDocumentId [String] The [documentId] of the document to be copied.
     * @param targetParentDocumentId [String] The [documentId] of the destination directory
     *   where the document will be copied.
     *
     * @return [String] The [documentId] of the newly created copy.
     *
     * @throws FileNotFoundException If the source document or target directory cannot be found,
     *   or if an [IOException] occurs during the file creation or copying process.
     *
     * @see asFile
     * @see resolveUnique
     */
    override fun copyDocument(sourceDocumentId: String, targetParentDocumentId: String) = try {
        val parent = targetParentDocumentId.asFile
        val oldFile = sourceDocumentId.asFile
        val newFile = parent.resolveUnique(oldFile.name)

        if (!(newFile.createNewFile() && newFile.setWritable(true) && newFile.setReadable(true))) throw IOException("Couldn't create new file")

        FileInputStream(oldFile).use { inStream -> FileOutputStream(newFile).use { outStream -> inStream.copyTo(outStream) } }

        newFile.documentId
    } catch (ex: IOException) {
        throw FileNotFoundException("Couldn't copy document '$sourceDocumentId': ${ex.message}")
    }

    /**
     * Moves a document from a source directory to a target directory.
     *
     * This operation is implemented as a `copy-then-delete` sequence. First, it calls
     * [copyDocument] to create a duplicate of the source document in the target
     * parent directory. If the copy is successful, it then calls [removeDocument] to
     * delete the original document from its source location.
     *
     * Before proceeding, it performs a sanity check using [isChildDocument] to ensure
     * that the specified [sourceParentDocumentId] is indeed the parent of the
     * [sourceDocumentId]. This prevents accidental moves from incorrect locations.
     *
     * @param sourceDocumentId [String] The [documentId] of the document to move.
     * @param sourceParentDocumentId [String] The [documentId] of the original parent directory.
     * @param targetParentDocumentId [String] The [documentId] of the destination directory.
     *
     * @return [String] The [documentId] of the newly created document in the target directory.
     *
     * @throws FileNotFoundException if the move fails at any step, such as if the
     *   parent-child relationship is invalid, the copy operation fails, or the subsequent
     *   deletion of the original document fails.
     *
     * @see copyDocument
     * @see removeDocument
     * @see isChildDocument
     */
    override fun moveDocument(sourceDocumentId: String, sourceParentDocumentId: String, targetParentDocumentId: String) = try {
        val newDocumentId = when {
            !isChildDocument(sourceParentDocumentId, sourceDocumentId) ->
                throw FileNotFoundException("Couldn't copy document '$sourceDocumentId' as its parent is not '$sourceParentDocumentId'")

            else                                                       -> copyDocument(sourceDocumentId, targetParentDocumentId)

        }
        removeDocument(sourceDocumentId, sourceParentDocumentId)

        newDocumentId
    } catch (ex: FileNotFoundException) {
        throw FileNotFoundException("Couldn't move document '$sourceDocumentId': ${ex.message}")
    }

    /**
     * Deletes a specified document.
     *
     * This method is called by the `SAF` when a client requests to delete a `file` or `directory`.
     * It first gets the [File] represented by [documentId] using [asFile] and then calls the
     * [File.delete] method on it. If the `file` is a non-empty `directory`, the deletion
     * may fail, as this method does not perform a recursive delete.
     *
     * For a version of this method that includes the parent document ID for validation, see
     * [removeDocument].
     *
     * @param documentId [String] The [documentId] of the document to be deleted.
     *
     * @throws FileNotFoundException If the document cannot be found or if the deletion fails
     *   for any reason (lack of permissions, or attempting to delete a non-empty directory).
     *
     * @see asFile
     * @see removeDocument
     */
    override fun deleteDocument(documentId: String) = when {
        !documentId.asFile.delete() -> throw FileNotFoundException("Couldn't delete document with ID '$documentId'")
        else                        -> Unit
    }

    /**
     * Deletes a document (`file` or `directory`) and verifies its parent.
     *
     * This method is an alternative to [deleteDocument] and is typically called
     * when the `SAF` has information about the document's parent. It performs an
     * additional safety check to ensure the document being deleted is indeed a
     * direct child of the specified [parentDocumentId].
     *
     * The method first gets both [File]s represented by their [documentId]s using [asFile].
     * It then verifies that the file to be deleted either:
     * - Is the parent itself (a no-op that resolves to a successful deletion).
     * - Has no parent directory.
     * - Has a parent directory that matches the provided parent.
     *
     * If these conditions are met and the file is successfully deleted, the method
     * completes. Otherwise, it throws an exception.
     *
     * @param documentId [String] The [documentId] of the document to be deleted.
     * @param parentDocumentId [String] The [documentId] of the expected parent directory.
     *
     * @throws FileNotFoundException if the document could not be found or if the deletion
     *   fails, for example due to file system permissions or an incorrect parent.
     *
     * @see asFile
     * @see deleteDocument
     */
    override fun removeDocument(documentId: String, parentDocumentId: String) {
        val parent = parentDocumentId.asFile
        val file = documentId.asFile
        val isDeleted = (parent == file || file.parentFile == null || file.parentFile == parent) && file.delete()

        if (!isDeleted) throw FileNotFoundException("Couldn't delete document with ID '$documentId'")
    }

    /**
     * Resolves a file name within this directory, automatically handling naming conflicts.
     *
     * This extension function is used when creating new `files` or `directories` to prevent
     * accidentally overwriting an existing one. If a file with the given [name] already
     * exists in the receiver directory, this function will append a numerical suffix
     * to the base name (before the extension) until an unused file name is found.
     *
     * @receiver [File] The parent directory in which to resolve the new file name
     *
     * @param name [String] The desired initial name for the file or directory.
     *
     * @return [File] A [File] object representing a path that does not currently exist. This
     *   will either be the original proposed path or a modified one with a numerical suffix.
     */
    private fun File.resolveUnique(name: String) = resolve(name).let {
        var file = it
        if (file.exists()) {
            var uniqueId = 1 // Makes sure two files don't have the same name by adding a number to the end of its name

            val name = it.nameWithoutExtension
            val extension = when {
                it.extension.isBlank() -> ""
                else -> ".${it.extension}"
            }

            while (file.exists()) {
                file = resolve("$name-$uniqueId$extension")
                uniqueId++
            }
        }

        file
    }

    /**
     * A helper function that adds a row to a [MatrixCursor] with metadata for a given document.
     *
     * This function is used by both [queryDocument] and [queryChildDocuments] to populate
     * the cursor with the necessary details for a `file` / `directory`. It can identify the
     * document to include either by its [documentId] or directly via a [File] object.
     *
     * It calculates the appropriate flags for the document based on its type (`file` / `directory`)
     * and its writability. For example, a writable directory gets the `FLAG_DIR_SUPPORTS_CREATE` flag,
     * while a writable file gets flags for `copy`, `move`, `rename`, etc. It also sets the
     * display name, size, MIME type, and last modified timestamp.
     *
     * @param cursor [MatrixCursor] The [MatrixCursor] to add the new row to.
     * @param documentId [documentId] The optional [documentId] of the file to include.
     *   If this is `null`, the [file] parameter must be provided.
     * @param file The optional [File] object to include. If this is `null`, the [documentId]
     *   parameter must be provided.
     *
     * @return [MatrixCursor] The modified [MatrixCursor] with the new row added.
     *
     * @throws FileNotFoundException If a [documentId] is provided but cannot be resolved to an existing file.
     */
    private fun includeFile(cursor: MatrixCursor, documentId: String?, file: File?) = cursor.newRow().run {
        val localDocumentId = documentId ?: file?.documentId ?: return@run cursor
        val localFile = file ?: localDocumentId.asFile

        val flags = when {
            localFile.isDirectory -> DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE

            else                          -> DocumentsContract.Document.FLAG_SUPPORTS_WRITE or
                    DocumentsContract.Document.FLAG_SUPPORTS_COPY or
                    DocumentsContract.Document.FLAG_SUPPORTS_MOVE or
                    DocumentsContract.Document.FLAG_SUPPORTS_RENAME or
                    DocumentsContract.Document.FLAG_SUPPORTS_REMOVE or
                    DocumentsContract.Document.FLAG_SUPPORTS_DELETE
        }

        val displayName = when (localFile) {
            baseDirectory -> context?.getString(R.string.app_name)
            else          -> localFile.name
        }

        add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, localDocumentId)
        add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName)
        add(DocumentsContract.Document.COLUMN_MIME_TYPE, localFile.mimeType)
        add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, localFile.lastModified())
        add(DocumentsContract.Document.COLUMN_SIZE, localFile.length())
        add(DocumentsContract.Document.COLUMN_FLAGS, flags)

        if (localFile == baseDirectory) add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher)

        cursor
    }
}
