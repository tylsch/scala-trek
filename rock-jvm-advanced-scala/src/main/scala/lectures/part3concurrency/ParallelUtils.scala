package lectures.part3concurrency

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicReference
import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable
import scala.collection.parallel.{ForkJoinTaskSupport, Task, TaskSupport}
import scala.collection.parallel.immutable._

object ParallelUtils extends App {
  // 1 - parallel collections
  val aParVector = ParVector[Int](1,2,3)

  def measure[T](operation: => T): Long = {
    val time = System.currentTimeMillis()
    operation
    System.currentTimeMillis() - time
  }

  val list = (1 to 10000000).toList
  val serialTime = measure {
    list.map(_ + 1)
  }
  val parallelTime = measure {
    list.par.map(_ + 1)
  }

  println(s"serial time $serialTime")
  println(s"parallel time $parallelTime")

  /*
  * Map-reduce model
  * - split the elements into chunks - Splitter
  * - operation
  * - recombine - Combiner
  * */

  // fold, reduce with non-associative operators
  println(List(1,2,3).reduce(_ - _))
  println(List(1,2,3).par.reduce(_ - _))

  // synchronization
  var sum = 0
  List(1,2,3).par.foreach(sum += _)
  println(sum) // race conditions

  // configuring
  aParVector.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(2))
  /*
  * Alternatives
  * - ThreadPoolTaskSupport == deprecated
  * - ExecutionContextTaskSupport(EC)
  * */

  aParVector.tasksupport = new TaskSupport {
    override val environment: AnyRef = ???
    override def execute[R, Tp](fjtask: Task[R, Tp]): () => R = ???
    override def executeAndWaitResult[R, Tp](task: Task[R, Tp]): R = ???
    override def parallelismLevel: Int = ???
  }

  // 2 - atomic ops an references
  val atomic = new AtomicReference[Int](2)
  val currentValue = atomic.get() // thread-safe read
  atomic.set(4) // thread-safe write
  atomic.getAndSet(5) // thread-safe read/write
  atomic.compareAndSet(38, 56) // if value is 38 then set value to 56, reference equality (shallow)

  atomic.updateAndGet(_ + 1) // thread-safe function run
  atomic.getAndUpdate(_ + 1) // reverse of updateAndGet

  atomic.accumulateAndGet(12, _ + _) // thread-safe accumulation
  atomic.getAndAccumulate(12, _ + _) // reverse
}
