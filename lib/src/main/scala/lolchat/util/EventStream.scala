package lolchat.util

import lolchat.model.Session
import io.dylemma.frp
import io.dylemma.frp._

import scala.concurrent.duration._

case class EventStream[+A](private val sess: Session, stream: frp.EventStream[A]) {

  def clear(): Unit = stream.clear()

  /** Add a handler function that acts as a `sink` for items produced by this
   * `Source`. The `handler` is expected to return `true` as long as it remains
   * active. Once the `handler` function returns `false` in response to some
   * produced item, it will be deactivated and will no longer receive new items.
   * There is no guarantee of the order that handlers will be called.
   *
   * @param handler The handler function to receive items produced by this `Source`.
   * Once the `handler` returns `false` in response to some produced item, it will
   * be deactivated and will no longer receive new items.
   */
  def sink(handler: Event[A] => Boolean): Unit = stream.sink(handler)(sess.obs)

  /** Marks whether or not this stream is stopped. A stopped stream will not
   * produce any more events.
 *
   * @return `true` if this stream is stopped, `false` otherwise.
   */
  def stopped: Boolean = stream.stopped


  /** Attach an event handler for data fired by this stream.
 *
   * @param f A function that takes in an event data and performs side effects.
   */
  def foreach[U](f: A => U): Unit = stream.foreach(f)(sess.obs)

  /** Assign a block of code that will run when this stream `stop`s. If this stream
   * is already stopped, the block of code will run immediately.
 *
   * @param f A block of code that will run when this stream sends a `Stop` event.
   */
  def onEnd(f: => Unit): Unit = stream.onEnd(f)(sess.obs)

  /** Assign a handler for the next event fired by this stream.
 *
   * @param f A function that takes in an event data and performs side effects.
   * It can be assumed that `f` will be run at most once.
   */
  def onNext(f: A => Unit): Unit = stream.onNext(f)(sess.obs)

  /** Creates a mapped version of this EventStream. For every event `e`,
   * fired by this stream, the mapped stream will fire an event equal
   * to `f(e)`. The mapped stream will stop with this stream stops.
   *
   * @param f The transformation function to be applied to events from this stream.
   * @return A new EventStream that fires events from this stream, transformed by `f`.
   */
  def map[B](f: A => B): frp.EventStream[B] = stream.map(f)

  /** Creates a new EventStream by applying a partial function to all events fired
   * by this stream on which the function is defined. The resulting stream will stop
   * when this stream stops.
   *
   * @param pf The partial function to apply to events from this stream
   * @return A new stream that fires events transformed by `pf`
   */
  def collect[B](pf: PartialFunction[A, B]): frp.EventStream[B] = stream.collect(pf)

  /** Creates a new EventStream with the following behavior: for every event
   * fired by this stream, a new stream will be created by applying `f` to that
   * event; events from the new stream will be fired by the resulting stream until
   * the next event from this stream, when the mapping re-starts. The resulting stream
   * will stop when this stream stops.
   *
   * @param f A function that returns a new EventStream for every event fired by this stream.
   * @return A new EventStream that fires events from the mapped streams, resetting the
   * mapped stream every time this stream fires a new event.
   */
  def flatMap[B](f: A => frp.EventStream[B]): frp.EventStream[B] = stream.flatMap(f)

  /** Creates a new EventStream that fires all events from this stream that match
   * the filter predicate. The resulting stream will stop when this stream stops.
   *
   * @param p The filter predicate. For each event `e`, fired by this stream, the
   * filtered stream will fire `e` as long as `p(e)` returns `true`.
   * @return A filtered version of this stream.
   */
  def withFilter(p: A => Boolean): frp.EventStream[A] = stream.withFilter(p)

  /** Alias for `withFilter` */
  def filter(p: A => Boolean): frp.EventStream[A] = stream.filter(p)

  /** Creates a new EventStream that updates its state for each new event fired by
   * this stream. The state starts at `z` and updates along the lines of
   * `state = op(state, event)` for every `event` fired by this stream. Each time
   * the state is updated, the new stream fires an event containing the state.
   *
   * @param z The initial state for the fold
   * @param op The update function, of the form `(state, next) => newState`
   * @return A new stream that fires events as its state updates according to `op`
   */
  def foldLeft[B](z: B)(op: (B, A) => B): frp.EventStream[B] = stream.foldLeft(z)(op)

  /** Creates a new EventStream that takes the first `count` events from this stream
   * and fires them, then stops. The resulting stream will also stop if this stream
   * stops anytime before it fires `count` new events.
   *
   * @param count The number of events that the resulting stream will re-fire
   * @return A new stream that re-fires the first `count` events from this stream.
   */
  def take(count: Int): frp.EventStream[A] = stream.take(count)

  /** Creates a new EventStream that re-fires events from this stream as long as the
   * event data satisfies `p`, the filter predicate. The first event `e` that causes
   * `p(e)` to be `false` will cause the resulting stream to stop. The new stream will
   * also stop if this stream is already stopped, or becomes stopped at any time.
   *
   * @param p The filter predicate function. Events fired by this stream will be passed
   * into `p`. As soon as the result is `false`, the new stream will stop.
   * @return A new stream that re-fires events from this stream until the filter
   * predicate fails for an event.
   */
  def takeWhile(p: A => Boolean): frp.EventStream[A] = stream.takeWhile(p)

  /** Creates a new EventStream that ignores the first `count` events fired by this stream,
   * then re-fires all events afterward. The resulting stream will stop when this stream does.
   *
   * @param count The number of events to ignore.
   * @return A new stream that fires all events from this stream after having ignored
   * `count` of them.
   */
  def drop(count: Int): frp.EventStream[A] = stream.drop(count)

  /** Creates a new EventStream that will re-fire all events fired by this stream, starting
   * as soon as the predicate function `p` returns `true` in response to one of the events.
   * All events prior to the first "passed" event will be ignored. The resulting stream
   * will stop when this stream stops.
   *
   * @param p The filter predicate function to evaluate events. Once this function returns
   * `true`, all events (including the current one) will be re-fired.
   * @return A new stream that ignores events until one of them causes `p` to return `true`.
   */
  def dropWhile(p: A => Boolean): frp.EventStream[A] = stream.dropWhile(p)

  /** Creates a new EventStream that represents the concatenation of this stream and `that`.
   * The resulting stream will re-fire all events from this stream at first. Once this stream
   * stops, the new stream will begin re-firing events from `that` stream, up until that one
   * stops as well. If both `this` and `that` streams are stopped at the time of creation,
   * the resulting stream will also be stopped.
   *
   * @param that Another EventStream whose events will be re-fired after this stream has stopped.
   * @return The concatenation of `this` and `that` event stream.
   */
  def ++[A1 >: A](that: frp.EventStream[A1]): frp.EventStream[A1] = stream ++ that

  /** Creates a new EventStream that will re-fire all events from this stream until the `end`
   * stream fires an event. The `end` stream stopping does not count as a fired event in this case.
   * The resulting stream will also stop when and if this stream stops.
   *
   * @param end An EventStream whose first event marks the end of the resulting stream
   * @return A new stream that re-fires events from this stream until the first event
   * from the `end` stream.
   */
  def until(end: frp.EventStream[_]): frp.EventStream[A] = stream.until(end)

  /** Creates a new EventStream that represents the union of `this` and `that` stream.
   * All events from both streams will be re-fired in the order that they are encountered.
   * The resulting stream will stop once both parent streams are stopped.
   *
   * @param that Any EventStream, to be joined with this stream in a Union.
   * @return The Union of this stream and `that` stream.
   */
  def ||[A1 >: A](that: frp.EventStream[A1]): frp.EventStream[A1] = stream || that

  /** Creates a new EventStream that fires all events from this stream as `Left`s, and all
   * events from `that` stream as `Right`s. It is essentially the same as a union, but
   * appropriate for when `this` and `that` are streams of different types. The resulting
   * stream will stop once both parent streams are stopped.
   *
   * @param that Any EventStream to be joined with this stream in an "Either" Union.
   * @return A new stream that fires events from `this` and `that` as `Either`s.
   */
  def either[B](that: frp.EventStream[B]): frp.EventStream[Either[A, B]] = stream.either(that)

  /** Creates a new EventStream that re-fires events from this stream that happen within
   * the given `duration` from the time of creation. The resulting stream will stop
   * when this stream stops, or when the `duration` runs out. Time-based expiration will
   * generally happen on another thread, as it is handled by a `ScheduledExecutorService`.
   *
   * @param duration The amount of time before the resulting stream stops automatically.
   * @return A new stream that represents all events fired by this stream, within the
   * given `duration` from the time of creation.
   */
  def within(duration: Duration): frp.EventStream[A] = stream.within(duration)

  /** Creates a new EventStream that re-fires events from this stream that happen before
   * the given `deadline`. The resulting stream will stop automatically when the deadline
   * expires, or when this stream stops. Time-based expiration will generally happen on
   * another thread, as it is handled by a `ScheduledExecutorService`.
   *
   * @param `deadline` A timestamp that tells when the resulting stream should stop.
   * @param A new stream that represents all events from this stream that happen before
   * the `deadline`.
   */
  def before(deadline: Deadline): frp.EventStream[A] = stream.before(deadline)

  /** Creates a new EventStream that re-fires events from this stream, paired with the
   * zero-based index of the event.
   */
  def zipWithIndex: frp.EventStream[(A, Int)] = stream.zipWithIndex

  /** Creates a new EventStream that re-fires events from this stream, paired with a
   * function that checks if that event is currently "stale". A stale event is one that
   * is not currently the most recent event fired by the stream.
   */
  def zipWithStaleness: frp.EventStream[(A, () => Boolean)] = stream.zipWithStaleness

  /** Creates a new EventStream that joins this stream with `that` stream, firing events
   * as pairs as soon as an event is available from both streams. The new stream will
   * buffer events from both parent streams, so take care to avoid creating a zipped stream
   * if one stream is expected to fire significantly more events than the other; the
   * buffer for the larger stream will continue to accumulate without being emptied.
   *
   * Example usage:
   * {{{
   * val a = EventSource[Int]
   * val b = EventSource[String]
   * val c: EventStream[(Int, String)] = a zip b
   *
   * c foreach println _
   *
   * a fire 5
   * a fire 2
   * a fire 4
   * b fire "A" // prints "(5, A)"
   * b fire "B" // prints "(2, B)"
   * b fire "C" // prints "(4, C)"
   * }}}
   *
   * @param that The event stream to be zipped with this stream.
   * @return A new stream that fires a pair for each corresponding pair of events from
   * `this` stream and `that` stream.
   */
  def zip[B](that: frp.EventStream[B]): frp.EventStream[(A, B)] = stream.zip(that)

  /** Where this EventStream's data can be represented as a `Tuple2[A1, A2]`, this
   * method creates two separate EventStreams that each represent a half of that pair,
   * respectively.
   *
   * Example usage:
   * {{{
   * val x = EventSource[(Int, String)]
   * val (a,b) = x.unzip
   *
   * for(i <- a) println("left: " + i)
   * for(s <- b) println("right: + s)
   *
   * x fire 1 -> "hi"
   * //prints "left: 1"
   * //prints "right: hi"
   *
   * x fire 5 -> "earth"
   * //prints "left: 5"
   * //prints "right: earth"
   * }}}
   *
   * @return Two new streams that represent the left and right halves of this stream's
   * events, respectively.
   */
  def unzip[A1, A2](implicit asPair: A => (A1, A2)): (frp.EventStream[A1], frp.EventStream[A2]) = stream.unzip

  /** Creates a new EventStream that accumulates events from this stream in a List,
   * re-firing the list once it reaches the specified `size` (or once this stream stops).
   * The behavior of the resulting stream is supposed to parallel the behavior of a
   * scala standard library collection's `grouped` method.
   *
   * @param size The group size
   * @return A new stream that buffers events into a list, firing them when the list
   * reaches the given `size` or when this stream stops.
   */
  def grouped(size: Int): frp.EventStream[List[A]] = stream.grouped(size)

}
