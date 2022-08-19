package part4sql

import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions._

object SparkSql extends App {
  val spark = SparkSession.builder()
    .appName("Spark SQL practice")
    .config("spark.master", "local")
    .config("spark.sql.warehouse.dir", "src/main/resources/warehouse")
    .getOrCreate()

  var carsDF = spark.read
    .option("inferSchema", "true")
    .json("src/main/resources/data/cars.json")

  // regular DF API
  carsDF.select(col("Name")).where(col("Origin") === "USA")

  // use Spark SQL
  carsDF.createOrReplaceTempView("cars")
  val americanCarsDF = spark.sql(
    """
      |select Name from cars where Origin = 'USA'
      |""".stripMargin)

  // we can run any SQL statement
  spark.sql("create database rtjvm")
  spark.sql("use rtjvm")
  val databasesDF = spark.sql("show databases")

  // transfer tables from a DB to Spark tables
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

  def transferTables(tableNames: List[String], shouldWriteToWarehouse: Boolean = false) = tableNames.foreach { tableName =>
    val tableDF = readTable(tableName)
    tableDF.createOrReplaceTempView(tableName)
    if (shouldWriteToWarehouse) {
      tableDF.write
        .mode(SaveMode.Overwrite)
        .saveAsTable(tableName)
    }
  }

  // Uncomment when first creating
  transferTables(List("employees", "departments", "dept_manager", "dept_emp", "salaries", "titles", "movies"))

  // read a DF from loaded tables
  //val employeesDF2 = spark.read.table("employees")

  /*
  * Exercises
  * - read the movies DFD and store it as a Spark table in the rtjvm DB
  * - count how many employees were hired in between 1/1/2000 and 1/1/2001
  * - show the average salaries for the employees hired between those dates grouped by department
  * - show the name of the best-paying department for employees in between those dates
  * */

  spark.sql(
    """
      |select count(*)
      |from employees
      |where hire_date > '1999-01-01' and hire_date < '2001-01-01'
      |""".stripMargin)

  spark.sql(
    """
      |select de.dept_no, avg(s.salary)
      |from employees e, dept_emp de, salaries s
      |where hire_date > '1999-01-01' and hire_date < '2001-01-01'
      |and e.emp_no = de.emp_no
      |and e.emp_no = s.emp_no
      |group by de.dept_no
      |""".stripMargin)

  spark.sql(
    """
      |select d.dept_name, avg(s.salary) payments
      |from employees e, dept_emp de, salaries s, departments d
      |where hire_date > '1999-01-01' and hire_date < '2001-01-01'
      |and e.emp_no = de.emp_no
      |and e.emp_no = s.emp_no
      |and d.dept_no = de.dept_no
      |group by d.dept_name
      |sort by payments
      |limit 1 desc
      |""".stripMargin)


}
