import java.io.File
fun main() { val f = File("/a/b"); val p = f.parentFile; val lf = File(p, "c"); println(lf.exists()) }
