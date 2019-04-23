package c2fo.coroutines

import kotlinx.coroutines.runBlocking


const val NUM_CHEFS = 8
const val PLATES_PER_CHEF = 6
const val MEAL_TIME_COST = 6000
const val TRAVERSE_COST = 100

interface RestaurantStrategy {
    suspend fun seat(id: Int)
    suspend fun orderAndCook(id: Int, numMeals: Int= 2)
    suspend fun feed(id: Int)
    suspend fun processOrder(id: Int)

}


fun main(args: Array<String>) {

    runBlocking {
        val strategy = MultiCookingPersonalChefRestaurantStrategy(8)
        (0 until 100).forEach {
            println("Processing order ${strategy.processOrder(it)}")
        }

    }
}
