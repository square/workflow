package com.squareup.workflow.diagnostic

import com.squareup.workflow.diagnostic.BrowseableDebugData.Data
import com.squareup.workflow.diagnostic.BrowseableDebugData.Node
import com.squareup.workflow.diagnostic.FakeFileSystem.MyPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.Buffer
import org.apache.sshd.common.NamedFactory
import org.apache.sshd.common.channel.ChannelListener
import org.apache.sshd.common.file.util.BaseFileSystem
import org.apache.sshd.common.file.util.BasePath
import org.apache.sshd.common.future.CloseFuture
import org.apache.sshd.common.future.SshFutureListener
import org.apache.sshd.common.session.SessionListener
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.UserAuth
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.subsystem.sftp.SftpEventListener
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory
import java.io.File
import java.io.FileNotFoundException
import java.lang.reflect.Proxy
import java.net.URI
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SeekableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.AccessMode
import java.nio.file.AccessMode.READ
import java.nio.file.CopyOption
import java.nio.file.DirectoryStream
import java.nio.file.DirectoryStream.Filter
import java.nio.file.FileStore
import java.nio.file.FileSystem
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileAttributeView
import java.nio.file.attribute.FileTime
import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.spi.FileSystemProvider
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

sealed class BrowseableDebugData {
  abstract val initializedTime: Long
  abstract val modifiedTime: Long

  data class Data(
    val description: String,
    override val initializedTime: Long = 0,
    override val modifiedTime: Long = 0
  ) : BrowseableDebugData()

  data class Node(
    val children: Map<String, () -> BrowseableDebugData> = emptyMap(),
    override val initializedTime: Long = 0,
    override val modifiedTime: Long = 0
  ) : BrowseableDebugData()
}

suspend fun serveDebugData(data: Flow<Node>) {
  DebugServer(data, coroutineContext).serve()
}

/**
 * TODO write documentation
 *
 * @param resolveDebugContext The [CoroutineContext] used to invoke [Node.children] factories.
 */
class DebugServer(
  private val debugData: Flow<Node>,
  resolveDebugContext: CoroutineContext,
  port: Int = 2222
) {

  @Volatile private lateinit var currentRoot: Node

  private val server = SshServer.setUpDefaultServer()
      .also { server ->
        val sftpSubSystemFactory = SftpSubsystemFactory()
            .also {
              it.addSftpEventListener(object : SftpEventListener by createLogProxy() {})
            }
        server.port = port
        server.keyPairProvider = SimpleGeneratorHostKeyProvider()
        server.userAuthFactories = mutableListOf<NamedFactory<UserAuth>>(UserAuthPasswordFactory())
        server.setFileSystemFactory {
          FakeFileSystem(FakeFileSystemProvider({ currentRoot }, resolveDebugContext))
        }
        server.subsystemFactories = mutableListOf<NamedFactory<Command>>(sftpSubSystemFactory)
        server.setPasswordAuthenticator { username, password, session ->
          println("authenticating: $username:$password")
          println("\tusername: ${session.username}")
          println("\tclient address: ${session.clientAddress}")
          println("\tlocal address: ${session.localAddress}")
          println("\t${session.attributesCount} attributes")
          println("\tproperties: ${session.properties}")
          true
        }
        server.addChannelListener(object : ChannelListener by createLogProxy() {})
        server.addSessionListener(object : SessionListener by createLogProxy() {})
      }

  suspend fun serve() {
    withContext(Dispatchers.IO) {
      val debugDataChannel = debugData.produceIn(this)
      currentRoot = debugDataChannel.receive()
      val subscription = launch {
        debugDataChannel.consumeEach { currentRoot = it }
      }

      println("Starting…")
      server.start()
      println("Started.")
      try {
        server.awaitClose()
      } catch (e: CancellationException) {
        println("Cancelled, stopping…")
        server.stop()
        throw e
      } finally {
        subscription.cancel()
        println("Stopped.")
      }
    }
  }
}

private suspend fun SshServer.awaitClose() = suspendCancellableCoroutine<Unit> { continuation ->
  val listener = SshFutureListener<CloseFuture> { continuation.resume(Unit) }
  addCloseFutureListener(listener)
  continuation.invokeOnCancellation { removeCloseFutureListener(listener) }
}

private class FakeFileSystem(
  fileSystemProvider: FileSystemProvider
) : BaseFileSystem<MyPath>(fileSystemProvider) {

  private inner class MyPath(
    root: String?,
    names: List<String>?
  ) : BasePath<MyPath, FakeFileSystem>(this, root, names) {
    override fun toRealPath(vararg options: LinkOption?): Path {
      println("toRealPath(${options.asList()})")
      throw UnsupportedOperationException()
    }

    override fun toFile(): File = File(toString())
  }

  override fun create(
    root: String?,
    names: MutableList<String>?
  ): MyPath {
    println("create($root, $names)")
    return MyPath(root, names)
  }

  override fun supportedFileAttributeViews(): MutableSet<String> {
    println("supportedFileAttributeViews()")
    return mutableSetOf("basic")
  }

  override fun isOpen(): Boolean {
    println("isOpen()")
    return true
  }

  override fun getUserPrincipalLookupService(): UserPrincipalLookupService {
    println("getUserPrincipalLookupService")
    throw UnsupportedOperationException()
  }

  override fun close() {
    println("close()")
  }
}

private class FakeFileSystemProvider(
  private val debugData: () -> BrowseableDebugData,
  private val resolveDebugContext: CoroutineContext
) : FileSystemProvider() {

  override fun checkAccess(
    path: Path,
    vararg modes: AccessMode
  ) {
    println("checkAccess($path, ${modes.asList()})")
    if (modes.any { it != READ }) {
      println("\taccess denied")
      throw AccessDeniedException(
          path.toFile(), reason = "$path only supports READ mode, requested ${modes.asList()}"
      )
    }
  }

  override fun copy(
    source: Path?,
    target: Path?,
    vararg options: CopyOption?
  ) {
    println("copy($source, $target, ${options.asList()})")
  }

  override fun <V : FileAttributeView?> getFileAttributeView(
    path: Path?,
    type: Class<V>?,
    vararg options: LinkOption?
  ): V? {
    println("getFileAttributeView($path, $type, ${options.asList()})")
    return null
  }

  override fun isSameFile(
    path: Path?,
    path2: Path?
  ): Boolean {
    println("isSameFile($path, $path2)")
    return path == path2
  }

  override fun newFileSystem(
    uri: URI?,
    env: MutableMap<String, *>?
  ): FileSystem {
    println("newFileSystem($uri, $env)")
    return FakeFileSystem(this)
  }

  override fun getScheme(): String {
    println("getScheme")
    return "fake"
  }

  override fun isHidden(path: Path?): Boolean {
    println("isHidden($path)")
    return false
  }

  override fun newDirectoryStream(
    dir: Path,
    filter: Filter<in Path>?
  ): DirectoryStream<Path> {
    println("newDirectoryStream($dir: ${dir::class}, $filter)")
    val node = resolveDebugData(dir)
    if (node !is Node) throw UnsupportedOperationException("Path is not a directory: $dir")
    return node.asDirectoryStream(dir, filter)
  }

  private fun resolveDebugData(path: Path) = runBlocking {
    withContext(resolveDebugContext) {
      debugData().resolve(path)
    }
  }

  override fun newByteChannel(
    path: Path,
    options: MutableSet<out OpenOption>?,
    vararg attrs: FileAttribute<*>
  ): SeekableByteChannel {
    println("newByteChannel($path, $options, ${attrs.asList()})")
    TODO()
  }

  override fun newFileChannel(
    path: Path,
    options: MutableSet<out OpenOption>?,
    vararg attrs: FileAttribute<*>
  ): FileChannel {
    println("newFileChannel($path, $options, ${attrs.asList()})")
    val node = resolveDebugData(path)
    if (node !is Data) throw UnsupportedOperationException("Path is not data: $path")
    return node.asFileChannel()
  }

  override fun delete(path: Path) {
    println("delete($path)")
    throw UnsupportedOperationException()
  }

  override fun <A : BasicFileAttributes?> readAttributes(
    path: Path,
    type: Class<A>,
    vararg options: LinkOption
  ): A {
    println("readAttributes($path, $type, ${options.asList()})")
    if (!type.isAssignableFrom(SimpleFileAttributes::class.java)) {
      throw UnsupportedOperationException("Unsupported attributes type: $type")
    }

    val node = resolveDebugData(path)
    return node.attributes as A
  }

  override fun readAttributes(
    path: Path,
    attributes: String,
    vararg options: LinkOption
  ): MutableMap<String, Any> {
    println("readAttributes($path, $attributes, ${options.asList()})")
    require("basic:" in attributes) { "Unsupported attributes query: $attributes" }
    return readAttributes<SimpleFileAttributes>(path, *options)
        .toMap()
        .toMutableMap()
        .also { println("returning attrs: $it") }
  }

  override fun getFileSystem(uri: URI?): FileSystem {
    println("getFileSystem($uri)")
    return FakeFileSystem(this)
  }

  override fun getPath(uri: URI): Path {
    println("getPath($uri)")
    TODO("not implemented")
  }

  override fun getFileStore(path: Path?): FileStore {
    println("getFileStore($path)")
    throw UnsupportedOperationException("file stores not supported")
  }

  override fun setAttribute(
    path: Path?,
    attribute: String?,
    value: Any?,
    vararg options: LinkOption?
  ) {
    println("setAttribute($path, $attribute, $value, ${options.asList()})")
    throw UnsupportedOperationException()
  }

  override fun move(
    source: Path,
    target: Path,
    vararg options: CopyOption?
  ) {
    println("move($source, $target, ${options.asList()})")
    throw UnsupportedOperationException()
  }

  override fun createDirectory(
    dir: Path,
    vararg attrs: FileAttribute<*>?
  ) {
    println("createDirectory($dir, ${attrs.asList()})")
    throw UnsupportedOperationException()
  }
}

data class SimpleFileAttributes(
  val size: Long,
  val fileKey: Any,
  val creationTime: FileTime = FileTime.fromMillis(0),
  val lastModifiedTime: FileTime = FileTime.fromMillis(0),
  val lastAccessTime: FileTime = FileTime.fromMillis(System.currentTimeMillis()),
  val other: Boolean = false,
  val directory: Boolean = false,
  val symbolicLink: Boolean = false,
  val regularFile: Boolean = false
) : BasicFileAttributes {
  override fun isOther(): Boolean = other
  override fun isDirectory(): Boolean = directory
  override fun isSymbolicLink(): Boolean = symbolicLink
  override fun isRegularFile(): Boolean = regularFile
  override fun creationTime(): FileTime = creationTime
  override fun size(): Long = size
  override fun fileKey(): Any = fileKey
  override fun lastModifiedTime(): FileTime = lastModifiedTime
  override fun lastAccessTime(): FileTime = lastAccessTime

  fun toMap(): Map<String, Any> = mapOf(
      "size" to size,
      "creationTime" to creationTime,
      "lastModifiedTime" to lastModifiedTime,
      "lastAccessTime" to lastAccessTime,
      "isOther" to other,
      "isSymbolicLink" to symbolicLink,
      "isRegularFile" to regularFile
  )
}

private inline fun <reified T : BasicFileAttributes> FileSystemProvider.readAttributes(
  path: Path,
  vararg options: LinkOption
): T = readAttributes(path, T::class.java, *options)

private inline fun <reified T> createLogProxy(): T =
  Proxy.newProxyInstance(
      DebugServer::class.java.classLoader,
      arrayOf(T::class.java)
  ) { _, method, args ->
    println("${method.name}(${args.asList()})")
  } as T

private fun Data.asFileChannel() = BufferFileChannel(
    Buffer()
        .apply { writeUtf8(description) }
        .readByteArray()
)

private fun Node.asDirectoryStream(
  dir: Path,
  filter: Filter<in Path>?
): DirectoryStream<Path> {
  val entries = children.map { (name, _) -> dir.resolve(name) }
      .filter { filter?.accept(it) ?: true }

  return object : DirectoryStream<Path> {
    override fun iterator(): MutableIterator<Path> = entries.toMutableList().iterator()

    override fun close() {
      // Noop
    }
  }
}

private class BufferFileChannel(private val buffer: ByteArray) : FileChannel() {
  override fun write(src: ByteBuffer): Int {
    println("write($src)")
    throw UnsupportedOperationException()
  }

  override fun write(
    srcs: Array<out ByteBuffer>,
    offset: Int,
    length: Int
  ): Long {
    println("write(${srcs.asList()}, $offset, $length")
    throw UnsupportedOperationException()
  }

  override fun write(
    src: ByteBuffer,
    position: Long
  ): Int {
    println("write($src, $position)")
    throw UnsupportedOperationException()
  }

  override fun force(metaData: Boolean) {
    println("force($metaData)")
  }

  override fun implCloseChannel() {
    println("implCloseChannel()")
  }

  override fun truncate(size: Long): FileChannel {
    println("truncate($size)")
    throw UnsupportedOperationException()
  }

  override fun lock(
    position: Long,
    size: Long,
    shared: Boolean
  ): FileLock {
    println("lock($position, $size, $shared)")
    throw UnsupportedOperationException()
  }

  override fun tryLock(
    position: Long,
    size: Long,
    shared: Boolean
  ): FileLock {
    println("tryLock($position, $size, $shared)")
    throw UnsupportedOperationException()
  }

  private var position = 0L

  override fun position(): Long = position

  override fun position(newPosition: Long): FileChannel {
    position = newPosition
    return this
  }

  override fun map(
    mode: MapMode?,
    position: Long,
    size: Long
  ): MappedByteBuffer {
    println("map($mode, $position, $size)")
    throw UnsupportedOperationException()
  }

  override fun size(): Long = buffer.size.toLong()

  override fun transferTo(
    position: Long,
    count: Long,
    target: WritableByteChannel
  ): Long {
    println("transferTo($position, $count, $target)")
    throw UnsupportedOperationException()
  }

  override fun read(dst: ByteBuffer): Int {
    println("read($dst)")
    return read(dst, position)
        .also { position += it }
  }

  override fun read(
    dsts: Array<out ByteBuffer>,
    offset: Int,
    length: Int
  ): Long {
    println("read(${dsts.asList()}, $offset, $length)")
    TODO("not implemented")
  }

  override fun read(
    dst: ByteBuffer,
    position: Long
  ): Int {
    println("read($dst, $position)")
    if (position >= buffer.size) return -1
    val count = minOf(dst.remaining(), (buffer.size - position).toInt())
    dst.put(buffer, position.toInt(), count)
    return count
  }

  override fun transferFrom(
    src: ReadableByteChannel,
    position: Long,
    count: Long
  ): Long {
    println("transferFrom($src, $position, $count)")
    throw UnsupportedOperationException()
  }
}

internal fun BrowseableDebugData.resolve(path: Path): BrowseableDebugData =
  path.fold(this) { subTree, subPath ->
    when (subTree) {
      is Data -> throw FileNotFoundException(path.toString())
      is Node -> subTree.children[subPath.toString()]?.invoke()
          ?: throw FileNotFoundException(path.toString())
    }
  }

private val BrowseableDebugData.attributes: SimpleFileAttributes
  get() = when (this) {
    is Data -> SimpleFileAttributes(
        size = description.length.toLong(),
        fileKey = this,
        regularFile = true,
        creationTime = FileTime.fromMillis(initializedTime),
        lastModifiedTime = FileTime.fromMillis(modifiedTime)
    )
    is Node -> SimpleFileAttributes(
        size = 0,
        fileKey = this,
        directory = true,
        creationTime = FileTime.fromMillis(initializedTime),
        lastModifiedTime = FileTime.fromMillis(modifiedTime)
    )
  }
