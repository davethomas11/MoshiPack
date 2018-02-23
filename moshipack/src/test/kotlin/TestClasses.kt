data class Pizza(var topping: String = "Stuff")

class PizzaPlus {
    var topping = "Stuff"
    var stuffed = true
}

data class ThePlug(var compact: Boolean = true, var schema: Int = 0)

data class SomeNumbers(var num1: Int, var num2: Float, var num3: Short, var num4: Double)

data class Nest(var eggs: List<Egg>)

data class Egg(var size: Int = 2)

data class Transients(var one: String, var three: String) {
    @Transient var two: String = "Transient"
}

// Does not work with KotlinJsonAdapterFactory
data class Transients2(var one:String, @Transient var two: String, var three: String)