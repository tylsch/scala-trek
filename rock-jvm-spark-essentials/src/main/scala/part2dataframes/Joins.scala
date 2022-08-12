package part2dataframes

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{col, expr, max}

object Joins extends App {
  val spark = SparkSession.builder()
    .appName("Joins")
    .config("spark.master", "local")
    .getOrCreate()

  val guitarsDF = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/guitars.json")

  val guitarPlayersDF = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/guitarPlayers.json")

  val bandsDF = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/bands.json")

  // joins
  val joinCondition = guitarPlayersDF.col("band") === bandsDF.col("id")
  val guitaristBandsDF = guitarPlayersDF
    .join(
      bandsDF,
      joinCondition,
      "inner"
    )

  // left outer join, with nulls where data is missing
  guitarPlayersDF
    .join(
      bandsDF,
      joinCondition,
      "left_outer"
    )

  //right outer join
  guitarPlayersDF
    .join(
      bandsDF,
      joinCondition,
      "right_outer"
    )

  // full outer join
  guitarPlayersDF
    .join(
      bandsDF,
      joinCondition,
      "full_outer"
    )

  // semi-joins = everything in the left DF for which there is a row in the right DF satisfying the condition
  guitarPlayersDF
    .join(
      bandsDF,
      joinCondition,
      "left_semi"
    )

  // anti-join = everything in the left DF for which there is a NO row in the right DF satisfying the condition
  guitarPlayersDF
    .join(
      bandsDF,
      joinCondition,
      "left_anti"
    )

  // things to bear in mind with joins
//  guitarPlayersDF.select("id", "band") // this crashes

  // option 1 - rename the column on which we are joining
  guitarPlayersDF
    .join(
      bandsDF.withColumnRenamed("id", "band"),
      "band"
    )

  // option 2 - drop the dup column
  guitaristBandsDF.drop(bandsDF.col("id"))

  // option 3 - rename the offending column and keep the data
  val bandsModDF = bandsDF.withColumnRenamed("id", "band")
  //guitarPlayersDF.join(bandsDF, guitarPlayersDF.col("band") === bandsModDF.col("id"))

  // using complex types
  guitarPlayersDF
    .join(
      guitarsDF.withColumnRenamed("id", "guitarId"),
      expr("array_contains(guitars, guitarId)")
    )

  /*
  * Exercise
  * 1) Show all employees with their MAX salaries
  * 2) Show all employees who were never managers
  * */

  def readTable(tableName: String) = spark.read
    .format("jdbc")
    .options(Map(
      "driver" -> "org.postgresql.Driver",
      "url" -> "jdbc:postgresql://localhost:5432/rtjvm",
      "user" -> "docker",
      "password" -> "docker",
      "dbtable" -> s"public.$tableName"
    ))
    .load()

  val employeesDF = readTable("employees")
  val salariesDF = readTable("salaries")
  val deptManagersDF = readTable("dept_manager")
  val titlesDF = readTable("titles")

  val maxSalariesPerEmpNoDF = salariesDF.groupBy("emp_no").agg(max("salary").as("maxSalary"))
  val employeesSalariesDF = employeesDF
    .join(
      maxSalariesPerEmpNoDF,
      "emp_no"
    )

  val nonManagersDF = employeesDF
    .join(
      deptManagersDF,
      employeesDF.col("emp_no") === deptManagersDF.col("emp_no"),
      "left_anti"
    )

  val mostRecentJobTitleDF = titlesDF.groupBy("emp_no", "title").agg(max("to_date"))
  val bestPaidEmployeesDF = employeesSalariesDF.orderBy(col("maxSalary").desc).limit(10)
  val bestPaidJobsDF = bestPaidEmployeesDF
    .join(mostRecentJobTitleDF, "emp_no")
  bestPaidJobsDF.show()
}
