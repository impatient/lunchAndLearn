package c2fo.coroutines

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * An attempt at letting a chef handle multiple diners when s/he is doing host/waitstaff duty
 *
 * This approach doesn't easily handle for concurrent requests, since each cook block is its own suspend function.
 * Would need a channel to better aggregate and make more efficient use of resources
 *
 */
class MultiCookingPersonalChefRestaurantStrategy(numChefs: Int = NUM_CHEFS) : RestaurantStrategy {

    val chefDispatcher = newFixedThreadPoolContext(numChefs, "CHEEEEEF")


    override suspend fun seat(id: Int) {
        withContext(chefDispatcher) {
            println("Seating group $id")
            delay(TRAVERSE_COST.toLong())
            println("Seated group $id")
        }

    }

    override suspend fun orderAndCook(id: Int, numMeals: Int) {
        withContext(chefDispatcher) {
            runBlocking {
                delay(TRAVERSE_COST.toLong()) // back to kitchen
            }
            println("Ordered meal for group")


            (0 until numMeals).map {

                launch(chefDispatcher) {
                    // force each meal to take exactly delay amount of time (otherwise delay is
                    // non-blocking for the whole thread
                    // at this point, still blocking, but using a different thread
                    runBlocking {
                        println("COOKING MEAL $it for $id")
                        delay(MEAL_TIME_COST.toLong())
                    }
                }
            }
        }
    }


    override suspend fun feed(id: Int) {
        delay(TRAVERSE_COST.toLong()) // return food to customer
        println("Delivered meals for group $id")

    }

    override suspend fun processOrder(id: Int) {
        coroutineScope {
            val startTime = System.currentTimeMillis()
            seat(id)
            orderAndCook(id, 2)
            feed(id)
            println("Elapsed time ${System.currentTimeMillis() - startTime}")
        }
    }
}

    fun main(args: Array<String>) {

        runBlocking {
            val strategy = MultiCookingPersonalChefRestaurantStrategy(1)
            (0 until 10).forEach {
                println("Processing order ${strategy.processOrder(it)}")
            }

        }
    }


