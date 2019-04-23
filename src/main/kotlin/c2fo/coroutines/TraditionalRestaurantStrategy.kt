package c2fo.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking

sealed class Customer

data class GroupSat(val id: Int, val count: Int = 2) : Customer()
data class GroupMeal(val id: Int, val mealIdx: Int) : Customer()

/**
 *  More traditional approach, thread per group
 */
class TraditionalRestaurantStrategy(numChefs: Int = NUM_CHEFS, numWaiters: Int = 2, numHosts: Int = 1) {

    val chefDispatcher = newFixedThreadPoolContext(numChefs, "CHEEEEEF")
    val waitDispatcher = newFixedThreadPoolContext(numWaiters, "WAITERS")

    val hostChannel = Channel<GroupSat>(Channel.UNLIMITED)
    val burnerChannel = Channel<GroupMeal>(numChefs * PLATES_PER_CHEF)
    val waitChannel = Channel<GroupSat>(Channel.UNLIMITED)
    val deliverChannel = Channel<GroupMeal>(Channel.UNLIMITED)


    fun CoroutineScope.seatGuests(numHosts: Int) {
        (0 until numHosts).forEach {
            launch {
                for (guest in hostChannel) {
                    runBlocking {
                        delay(TRAVERSE_COST.toLong())
                        waitChannel.send(guest)
                    }
                }
            }
        }
    }

    fun CoroutineScope.orderIn(numChefs: Int) = (0 until numChefs * PLATES_PER_CHEF)
        .forEach {
            launch {
                for (meal in burnerChannel) {
                    println("Starting cooking")
                    delay(MEAL_TIME_COST.toLong())
                    println("Finish cooking")
                    deliverChannel.send(meal)
                }
            }
        }

    fun CoroutineScope.delivery(numWaiters: Int) =
        (0 until numWaiters).forEach {
            launch {
                for (meal in deliverChannel) {
                    runBlocking {
                        delay(TRAVERSE_COST.toLong())
                        println("Meal delivered $meal")

                    }
                }
            }
        }


    fun CoroutineScope.takeOrder(numWaiters: Int) =
        (0 until numWaiters).forEach {
            launch {
                for (meal in waitChannel) {
                    runBlocking {
                        delay(TRAVERSE_COST.toLong())
                        (0 until meal.count).map {
                            println("Ordering for ${meal.id}")
                            burnerChannel.send(GroupMeal(meal.id, it))
                        }
                    }
                }
            }
        }


    val jorbs: List<Job>

    init {
        jorbs = listOf(GlobalScope.launch(chefDispatcher) {
            orderIn(numChefs)
        }.apply {
            invokeOnCompletion {
                deliverChannel.close()
            }
        },
            GlobalScope.launch(waitDispatcher) {
                takeOrder(numWaiters)
            }.apply {
                invokeOnCompletion {
                    burnerChannel.close()
                }
            },
            GlobalScope.launch(Dispatchers.IO) {
                seatGuests(numHosts)
            }.apply {
                invokeOnCompletion {
                    waitChannel.close()
                }
            },
            GlobalScope.launch(waitDispatcher) {
                delivery(numWaiters)
            })


    }

    suspend fun processOrder(id: Int) {
        coroutineScope {
            hostChannel.send(GroupSat(id))
        }
    }

    fun noMoreGuests() {
        hostChannel.close()
    }

    suspend fun finish() {
        jorbs.joinAll()
    }

}


fun main(args: Array<String>) {


    runBlocking {
        val startTime = System.currentTimeMillis()
        val strategy = TraditionalRestaurantStrategy(1, 1, 1)

        (0 until 12).forEach {
            println("Processing order ${strategy.processOrder(it)}")
        }

        strategy.noMoreGuests()

        strategy.finish() // ideally wrap all of this in a suspend function and don't have to wait on jobs
        println("Elapsed time ${System.currentTimeMillis() - startTime}")
    }
}