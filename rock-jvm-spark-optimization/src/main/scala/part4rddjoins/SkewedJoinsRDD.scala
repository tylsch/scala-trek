package part4rddjoins

import generator.{DataGenerator, Laptop, LaptopOffer}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

object SkewedJoinsRDD {
  val spark: SparkSession = SparkSession.builder()
    .master("local[*]")
    .appName("RDD Skewed Joins")
    //    .config("spark.sql.autoBroadcastJoinThreshold", -1)
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  val sc: SparkContext = spark.sparkContext

  /*
    * An online store selling gaming laptops
    * 2 laptops are "similar" if they have same make and model, but processing speed within 0.1
    *
    * For each laptop configuration, we are interested in the average sale price of "similar" models
    *
    * Acer Predator 2.9Ghz soiruptweiuv -> average sale price of all Acer Predators with CPU speed between 2.8 and 3.0 Ghz
    * */

  val laptops: RDD[Laptop] = sc.parallelize(Seq.fill(40000)(DataGenerator.randomLaptop()))
  val laptopOffers: RDD[LaptopOffer] = sc.parallelize(Seq.fill(100000)(DataGenerator.randomLaptopOffer()))

  def plainJoin() = {
    val preparedLaptops = laptops.map {
      case Laptop(registration, make, model, procSpeed) => ((make, model), (registration, procSpeed))
    }
    val preparedOffers = laptopOffers.map {
      case LaptopOffer(make, model, procSpeed, salePrice) => ((make, model), (procSpeed, salePrice))
    }

    val result: RDD[(String, Double)] = preparedLaptops
      .join(preparedOffers)
      .filter {
        case ((make, model), ((reg, laptopCpu), (offerCpu, salePrice))) => Math.abs(laptopCpu - offerCpu) <= 0.1
      }
      .map {
        case ((make, model), ((reg, laptopCpu), (offerCpu, salePrice))) => (reg, salePrice)
      }
    // groupBy(reg).avg(salePrice)
      .aggregateByKey((0.0, 0))(
        {
          case ((totalPrice, numPrice), salePrice) => (totalPrice + salePrice, numPrice + 1) // combine state with record
        },
        {
          case ((totalPrice1, numPrices1), (totalPrices2, numPrices2)) => (totalPrice1 + totalPrices2, numPrices1 + numPrices2) // combine two states into one
        }
      )
      .mapValues {
        case (totalPrices, numPrices) => totalPrices / numPrices
      }

    result.count()
  }

  def noSkewJoin() = {
    val preparedLaptops = laptops
      .flatMap { laptop =>
        Seq(
          laptop,
          laptop.copy(procSpeed = laptop.procSpeed - 0.1),
          laptop.copy(procSpeed = laptop.procSpeed + 0.1)
        )
      }
      .map {
        case Laptop(registration, make, model, procSpeed) => ((make, model, procSpeed), registration)
      }
    val preparedOffers = laptopOffers.map {
      case LaptopOffer(make, model, procSpeed, salePrice) => ((make, model, procSpeed), salePrice)
    }

    val result = preparedLaptops
      .join(preparedOffers)
      .map(_._2)
      .aggregateByKey((0.0, 0))(
        {
          case ((totalPrice, numPrice), salePrice) => (totalPrice + salePrice, numPrice + 1) // combine state with record
        },
        {
          case ((totalPrice1, numPrices1), (totalPrices2, numPrices2)) => (totalPrice1 + totalPrices2, numPrices1 + numPrices2) // combine two states into one
        }
      )
      .mapValues {
        case (totalPrices, numPrices) => totalPrices / numPrices
      }

    result.count()
  }

  def main(args: Array[String]): Unit = {
    noSkewJoin()
    Thread.sleep(10000000)
  }
}
