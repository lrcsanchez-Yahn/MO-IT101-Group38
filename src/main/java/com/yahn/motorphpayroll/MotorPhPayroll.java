/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.yahn.motorphpayroll;

/**
 *
 * @author yahns
 */
import java.io.*;
import java.util.*;

public class MotorPhPayroll {

    public static void main(String[] args) throws Exception {

        // Scanner for user input
        Scanner sc = new Scanner(System.in);

        // ===============================
        // MOTORPH SYSTEM HEADER
        // ===============================
        System.out.println("======================================");
        System.out.println("        MOTORPH PAYROLL SYSTEM        ");
        System.out.println("======================================");

        // ===============================
        // LOGIN SYSTEM Only two valid users: employee & payroll_staff
        // ===============================
        System.out.print("Username: ");
        String username = sc.nextLine();

        System.out.print("Password: ");
        String password = sc.nextLine();

        // Validate credentials
        if (!(username.equals("employee") || username.equals("payroll_staff"))
                || !password.equals("12345")) {

            System.out.println("Incorrect username and/or password.");
            return;
        }

        // ==================================================
        // EMPLOYEE ACCESS Allows employee to view their personal information
        // ==================================================
        if (username.equals("employee")) {

            System.out.println("\n1. Enter your employee number");
            System.out.println("2. Exit");

            int choice = getChoice(sc,1,2);
            if(choice == 2) return;

            System.out.print("Enter employee number: ");
            String empNum = sc.next();

            BufferedReader br = new BufferedReader(new FileReader("employees.csv"));
            br.readLine(); // skip header row

            String line;
            boolean found = false;

            // Search employee in CSV file
            while((line = br.readLine()) != null){

                if(line.trim().isEmpty()) continue;

                String[] emp = line.split(",");
                if(emp.length < 19) continue;

                if(emp[0].equals(empNum)){

                    System.out.println("\nEmployee Number : " + emp[0]);
                    System.out.println("Employee Name   : " + emp[2] + " " + emp[1]);
                    System.out.println("Birthday        : " + emp[3]);

                    found = true;
                    break;
                }
            }

            if(!found)
                System.out.println("Employee number does not exist.");

            br.close();
            return;
        }

        // ==================================================
        // PAYROLL STAFF ACCESS Staff can process payroll
        // ==================================================
        System.out.println("\n1. Process Payroll");
        System.out.println("2. Exit");

        int mainChoice = getChoice(sc,1,2);
        if(mainChoice == 2) return;

        System.out.println("\n1. One employee");
        System.out.println("2. All employees");
        System.out.println("3. Exit");

        int option = getChoice(sc,1,3);
        if(option == 3) return;

        String targetEmployee = "";

        // If payroll for only one employee
        if(option == 1){
            System.out.print("Enter employee number: ");
            targetEmployee = sc.next();
        }

        BufferedReader empReader = new BufferedReader(new FileReader("employees.csv"));
        empReader.readLine(); // skip header

        String empLine;
        boolean foundEmployee = false;

        // ==================================================
        // READ EMPLOYEE DATA
        // ==================================================
        while((empLine = empReader.readLine()) != null){

            if(empLine.trim().isEmpty()) continue;

            String[] emp = empLine.split(",");
            if(emp.length < 19) continue;

            String empNum = emp[0];
            String empName = emp[2] + " " + emp[1];
            String birthday = emp[3];

            // Read hourly rate from the LAST column
            // Remove quotes and commas if Excel added them
            String rateStr = emp[emp.length-1].replace("\"","").replace(",","").trim();
            double rate = Double.parseDouble(rateStr);

            if(option == 1 && !empNum.equals(targetEmployee))
                continue;

            foundEmployee = true;

            // Arrays to store hours per month
            double[] firstHours = new double[13];
            double[] secondHours = new double[13];

            // ==================================================
            // READ ATTENDANCE DATA
            // ==================================================
            BufferedReader attReader = new BufferedReader(new FileReader("attendance.csv"));
            attReader.readLine(); // skip header

            String attLine;

            while((attLine = attReader.readLine()) != null){

                if(attLine.trim().isEmpty()) continue;

                String[] att = attLine.split(",");
                if(att.length < 6) continue;

                if(!att[0].equals(empNum)) continue;

                String date = att[3];

                // Extract month and day from date
                int month = Integer.parseInt(date.substring(0,2));
                int day = Integer.parseInt(date.substring(3,5));

                // Only include June to December records
                if(month < 6 || month > 12) continue;

                // Compute hours worked
                double hours = computeHours(att[4], att[5]);

                // Determine cutoff period
                if(day <= 15)
                    firstHours[month] += hours;
                else
                    secondHours[month] += hours;
            }

            attReader.close();

            // ==================================================
            // PROCESS PAYROLL PER MONTH
            // ==================================================
            for(int m = 6; m <= 12; m++){

                if(firstHours[m] == 0 && secondHours[m] == 0)
                    continue;

                // Compute gross salaries
                double firstGross = firstHours[m] * rate;
                double secondGross = secondHours[m] * rate;

                double totalGross = firstGross + secondGross;

                // Government deductions
                double sss = computeSSS(totalGross);
                double philhealth = computePhilHealth(totalGross);
                double pagibig = computePagibig(totalGross);

                // Compute taxable income
                double taxableIncome = totalGross - (sss + philhealth + pagibig);

                // Compute withholding tax
                double tax = computeTax(taxableIncome);

                double totalDeduction = sss + philhealth + pagibig + tax;

                // Net salary
                double firstNet = firstGross;
                double secondNet = secondGross - totalDeduction;

                // Display payroll
                printPayroll(empNum, empName, birthday,
                        m, rate,
                        firstHours[m], secondHours[m],
                        firstGross, secondGross,
                        sss, philhealth, pagibig, tax, totalDeduction,
                        firstNet, secondNet);
            }

            if(option == 1) break;
        }

        if(option == 1 && !foundEmployee)
            System.out.println("Employee number does not exist.");

        empReader.close();
    }

    // ==================================================
    // COMPUTE HOURS WORKED Only counts work between 8:00 AM and 5:00 PM
    // ==================================================
    public static double computeHours(String in,String out){

        String[] t1 = in.split(":");
        String[] t2 = out.split(":");

        double inTime = Integer.parseInt(t1[0]) + Integer.parseInt(t1[1]) / 60.0;
        double outTime = Integer.parseInt(t2[0]) + Integer.parseInt(t2[1]) / 60.0;

        if(inTime < 8) inTime = 8;
        if(outTime > 17) outTime = 17;

        // Special rule: 8:05 AM still considered 8:00
        if(inTime > 8 && inTime <= 8.0833)
            inTime = 8;

        if(outTime < inTime)
            return 0;

        return outTime - inTime;
    }

    // ==================================================
    // SSS CONTRIBUTION TABLE
    // ==================================================
    public static double computeSSS(double salary){

        if(salary < 3250) return 135;
        else if(salary < 3750) return 157.5;
        else if(salary < 4250) return 180;
        else if(salary < 4750) return 202.5;
        else if(salary < 5250) return 225;
        else if(salary < 5750) return 247.5;
        else if(salary < 6250) return 270;
        else if(salary < 6750) return 292.5;
        else if(salary < 7250) return 315;
        else if(salary < 7750) return 337.5;
        else if(salary < 8250) return 360;
        else if(salary < 8750) return 382.5;
        else if(salary < 9250) return 405;
        else if(salary < 9750) return 427.5;
        else if(salary < 10250) return 450;
        else return 1125;
    }

    // ==================================================
    // PHILHEALTH CONTRIBUTION
    // ==================================================
    public static double computePhilHealth(double salary){
        double premium;
        if(salary <= 10000) premium = 300;
        else if(salary < 60000) premium = salary * 0.03;
        else premium = 1800;
        return premium / 2;
    }

    // ==================================================
    // PAG-IBIG CONTRIBUTION
    // ==================================================
    public static double computePagibig(double salary){
        double contribution;
        if(salary <= 1500) contribution = salary * 0.01;
        else contribution = salary * 0.02;
        if(contribution > 100) contribution = 100;
        return contribution;
    }

    // ==================================================
    // WITHHOLDING TAX TABLE
    // ==================================================
    public static double computeTax(double income){
        if(income <= 20832) return 0;
        else if(income < 33333) return (income - 20833) * 0.20;
        else if(income < 66667) return 2500 + (income - 33333) * 0.25;
        else return 10833 + (income - 66667) * 0.30;
    }

    // ==================================================
    // VALIDATE MENU INPUT
    // ==================================================
    public static int getChoice(Scanner sc,int min,int max){

        int choice = -1;

        while(choice < min || choice > max){

            System.out.print("Select option ("+min+"-"+max+"): ");

            if(sc.hasNextInt()){
                choice = sc.nextInt();
                sc.nextLine();
            }
            else{
                System.out.println("Invalid input.");
                sc.next();
            }
        }

        return choice;
    }

    // ==================================================
    // DISPLAY PAYROLL INFORMATION
    // ==================================================
    public static void printPayroll(
            String empNum,String name,String birthday,
            int month,double rate,
            double h1,double h2,
            double g1,double g2,
            double sss,double ph,double pagibig,double tax,double totalDed,
            double net1,double net2){

        String[] months = {"","January","February","March","April","May",
                "June","July","August","September","October","November","December"};

        System.out.println("\n======================================================");
        System.out.println("Month: " + months[month]);
        System.out.println("======================================================");

        System.out.println("Employee #: " + empNum);
        System.out.println("Employee Name: " + name);
        System.out.println("Birthday: " + birthday);
        System.out.printf("Hourly Rate: %.2f\n", rate);

        System.out.println("\nCutoff Date: " + months[month] + " 1–15");
        System.out.printf("Total Hours Worked: %.2f\n", h1);
        System.out.printf("Gross Salary: %.2f\n", g1);
        System.out.printf("Net Salary: %.2f\n", net1);

        System.out.println("\nCutoff Date: " + months[month] + " 16–End");
        System.out.printf("Total Hours Worked: %.2f\n", h2);
        System.out.printf("Gross Salary: %.2f\n", g2);

        System.out.printf("SSS: %.2f\n", sss);
        System.out.printf("PhilHealth: %.2f\n", ph);
        System.out.printf("Pag-IBIG: %.2f\n", pagibig);
        System.out.printf("Tax: %.2f\n", tax);

        System.out.printf("Total Deductions: %.2f\n", totalDed);
        System.out.printf("Net Salary: %.2f\n", net2);
    }
}