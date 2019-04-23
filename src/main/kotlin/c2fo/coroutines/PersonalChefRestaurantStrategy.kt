package c2fo.coroutines

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PersonalChefRestaurantStrategy(numChefs: Int = NUM_CHEFS) : RestaurantStrategy {

    val chefDispatcher = newFixedThreadPoolContext(numChefs, "CHEEEEEF")

    override suspend fun seat(id: Int) {
        println("Seating group $id")
        runBlocking(chefDispatcher) {
            delay(TRAVERSE_COST.toLong())
            println("Seated group $id")
        }
    }

    override suspend fun orderAndCook(id: Int, numMeals: Int) {
        delay(TRAVERSE_COST.toLong()) // back to kitchen
        withContext(chefDispatcher) {
            (0 until numMeals).map {
                async {
                    // async needed to allow multiple threads to be used (otherwise, the delay is non-blocking)
                    // see note about concurrency not parallelism.
                    runBlocking {
                        // force each meal to take exactly delay amount of time (otherwise delay is
                        // non-blocking for the whole thread
                        println("COOKING MEAL $it for $id")
                        delay(MEAL_TIME_COST.toLong())
                    }
                }
            }.awaitAll()
        }
    }

    override suspend fun feed(id: Int) {
        runBlocking(chefDispatcher) {
            delay(TRAVERSE_COST.toLong()) // return food to customer
            println("Delivered meals for group $id")
        }
    }

    override suspend fun processOrder(id: Int) {
        coroutineScope {
            val startTime = System.currentTimeMillis()
            seat(id)
            orderAndCook(id,2)
            feed(id)
            println("Elapsed time ${System.currentTimeMillis() - startTime}")
        }
    }

}

fun main(args: Array<String>) {

    runBlocking {
        val strategy = PersonalChefRestaurantStrategy(1)
        (0 until 100).forEach {
            println("Processing order ${strategy.processOrder(it)}")
        }

    }
}