/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.yahn.motorphpayroll;

import java.io.*;
import java.util.*;

/*
 ==============================================================
 MOTORPH PAYROLL SYSTEM
 ==============================================================

 This program simulates a payroll system for MotorPH.

 Main Functions:
 1. Login authentication (employee or payroll staff)
 2. Employees can view their personal information
 3. Payroll staff can process payroll for one or all employees

 Data Sources:
 - employees.csv  (employee details and hourly rate)
 - attendance.csv (daily time records)

 Payroll Rules:
 - Only work between 8:00 AM and 5:00 PM is counted
 - A 1-hour lunch break is deducted when applicable
 - Payroll is processed dynamically based on attendance data
 - No rounding is applied to payroll computations

 The system reads attendance records once and reuses them
 to improve performance and avoid repeated file access.
 ============================================================== 
*/

public class MotorPhPayroll {

    // Constants replace magic numbers for readability
    static final double START_WORK = 8.0;
    static final double END_WORK = 17.0;
    static final double LUNCH_BREAK = 1.0;

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        System.out.println("======================================");
        System.out.println("        MOTORPH PAYROLL SYSTEM        ");
        System.out.println("======================================");

        // =========================
        // LOGIN SYSTEM
        // =========================
        System.out.print("Username: ");
        String username = sc.nextLine();

        System.out.print("Password: ");
        String password = sc.nextLine();

        // Only two valid accounts
        if (!(username.equals("employee") || username.equals("payroll_staff"))
                || !password.equals("12345")) {

            System.out.println("Incorrect username and/or password.");
            return;
        }

        // =========================
        // EMPLOYEE VIEW
        // =========================
        if (username.equals("employee")) {
            handleEmployeeView(sc);
            return;
        }

        // =========================
        // PAYROLL STAFF MENU
        // =========================
        int option = handlePayrollMenu(sc);

        String targetEmployee = "";

        if (option == 1) {
            System.out.print("Enter employee number: ");
            targetEmployee = sc.next();
        }

        // Load attendance data once (performance improvement)
        List<String[]> attendanceData = loadAttendance();

        processPayroll(option, targetEmployee, attendanceData);
    }

    // ==================================================
    // EMPLOYEE VIEW
    // ==================================================
    public static void handleEmployeeView(Scanner sc) throws Exception {

        System.out.println("\n1. Enter your employee number");
        System.out.println("2. Exit");

        int choice = getChoice(sc, 1, 2);

        if (choice == 2)
            return;

        System.out.print("Enter employee number: ");
        String empNum = sc.next();

        BufferedReader br = new BufferedReader(new FileReader("employees.csv"));
        br.readLine();

        String line;
        boolean found = false;

        while ((line = br.readLine()) != null) {

            if (line.trim().isEmpty())
                continue;

            String[] emp = line.split(",");

            if (emp[0].equals(empNum)) {

                System.out.println("\nEmployee Number : " + emp[0]);
                System.out.println("Employee Name   : " + emp[2] + " " + emp[1]);
                System.out.println("Birthday        : " + emp[3]);

                found = true;
                break;
            }
        }

        if (!found)
            System.out.println("Employee number does not exist.");

        br.close();
    }

    // ==================================================
    // PAYROLL MENU
    // ==================================================
    public static int handlePayrollMenu(Scanner sc) {

        System.out.println("\n1. Process Payroll");
        System.out.println("2. Exit");

        int mainChoice = getChoice(sc, 1, 2);

        if (mainChoice == 2)
            return 3;

        System.out.println("\n1. One employee");
        System.out.println("2. All employees");
        System.out.println("3. Exit");

        return getChoice(sc, 1, 3);
    }

    // ==================================================
    // LOAD ATTENDANCE DATA ONCE
    // ==================================================
    public static List<String[]> loadAttendance() throws Exception {

        List<String[]> attendanceList = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader("attendance.csv"));
        reader.readLine();

        String line;

        while ((line = reader.readLine()) != null) {

            if (!line.trim().isEmpty())
                attendanceList.add(line.split(","));
        }

        reader.close();

        return attendanceList;
    }

    // ==================================================
    // PROCESS PAYROLL
    // ==================================================
    public static void processPayroll(int option, String targetEmployee,
                                      List<String[]> attendanceData) throws Exception {

        BufferedReader empReader = new BufferedReader(new FileReader("employees.csv"));
        empReader.readLine();

        String empLine;
        boolean foundEmployee = false;

        while ((empLine = empReader.readLine()) != null) {

            if (empLine.trim().isEmpty())
                continue;

            String[] emp = empLine.split(",");

            String empNum = emp[0];
            String empName = emp[2] + " " + emp[1];
            String birthday = emp[3];

            double rate = Double.parseDouble(emp[emp.length - 1]
                    .replace("\"", "")
                    .replace(",", ""));

            if (option == 1 && !empNum.equals(targetEmployee))
                continue;

            foundEmployee = true;

            double[] firstHours = new double[13];
            double[] secondHours = new double[13];

            // Process attendance records
            for (String[] att : attendanceData) {

                if (!att[0].equals(empNum))
                    continue;

                String date = att[3];

                int month = Integer.parseInt(date.substring(0, 2));
                int day = Integer.parseInt(date.substring(3, 5));

                double hours = computeHours(att[4], att[5]);

                // Separate first and second payroll cutoff
                if (day <= 15)
                    firstHours[month] += hours;
                else
                    secondHours[month] += hours;
            }

            computeMonthlyPayroll(empNum, empName, birthday,
                    rate, firstHours, secondHours);

            if (option == 1)
                break;
        }

        if (option == 1 && !foundEmployee)
            System.out.println("Employee number does not exist.");

        empReader.close();
    }

    // ==================================================
    // COMPUTE MONTHLY PAYROLL
    // ==================================================
    public static void computeMonthlyPayroll(String empNum, String name,
                                             String birthday, double rate,
                                             double[] firstHours,
                                             double[] secondHours) {

        // Iterate through all months (future-proof design)
        for (int m = 1; m <= 12; m++) {

            if (firstHours[m] == 0 && secondHours[m] == 0)
                continue;

            double firstGross = firstHours[m] * rate;
            double secondGross = secondHours[m] * rate;

            double totalGross = firstGross + secondGross;

            double sss = computeSSS(totalGross);
            double philhealth = computePhilHealth(totalGross);
            double pagibig = computePagibig(totalGross);

            double taxableIncome = totalGross - (sss + philhealth + pagibig);

            double tax = computeTax(taxableIncome);

            double totalDeduction = sss + philhealth + pagibig + tax;

            double firstNet = firstGross;
            double secondNet = secondGross - totalDeduction;

            printPayroll(empNum, name, birthday, m, rate,
                    firstHours[m], secondHours[m],
                    firstGross, secondGross,
                    sss, philhealth, pagibig, tax,
                    totalDeduction, firstNet, secondNet);
        }
    }

    // ==================================================
    // COMPUTE HOURS WORKED
    // ==================================================
    public static double computeHours(String in, String out) {

        String[] t1 = in.split(":");
        String[] t2 = out.split(":");

        double inTime = Integer.parseInt(t1[0]) + Integer.parseInt(t1[1]) / 60.0;
        double outTime = Integer.parseInt(t2[0]) + Integer.parseInt(t2[1]) / 60.0;

        if (inTime < START_WORK)
            inTime = START_WORK;

        if (outTime > END_WORK)
            outTime = END_WORK;

        if (inTime > 8 && inTime <= 8.0833)
            inTime = 8;

        if (outTime <= inTime)
            return 0;

        double hours = outTime - inTime;

        if (hours > 4)
            hours -= LUNCH_BREAK;

        return hours;
    }

    // ==================================================
    // SSS CONTRIBUTION
    // ==================================================
    public static double computeSSS(double salary) {

        if (salary < 3250) return 135;
        else if (salary < 3750) return 157.5;
        else if (salary < 4250) return 180;
        else if (salary < 4750) return 202.5;
        else if (salary < 5250) return 225;
        else if (salary < 5750) return 247.5;
        else if (salary < 6250) return 270;
        else if (salary < 6750) return 292.5;
        else if (salary < 7250) return 315;
        else if (salary < 7750) return 337.5;
        else if (salary < 8250) return 360;
        else if (salary < 8750) return 382.5;
        else if (salary < 9250) return 405;
        else if (salary < 9750) return 427.5;
        else if (salary < 10250) return 450;
        else return 1125;
    }

    // ==================================================
    // PHILHEALTH CONTRIBUTION
    // ==================================================
    public static double computePhilHealth(double salary) {

        double premium;

        if (salary <= 10000)
            premium = 300;
        else if (salary < 60000)
            premium = salary * 0.03;
        else
            premium = 1800;

        return premium / 2;
    }

    // ==================================================
    // PAG-IBIG CONTRIBUTION
    // ==================================================
    public static double computePagibig(double salary) {

        double contribution;

        if (salary <= 1500)
            contribution = salary * 0.01;
        else
            contribution = salary * 0.02;

        if (contribution > 100)
            contribution = 100;

        return contribution;
    }

    // ==================================================
    // WITHHOLDING TAX
    // ==================================================
    public static double computeTax(double income) {

        if (income <= 20832) return 0;
        else if (income < 33333) return (income - 20833) * 0.20;
        else if (income < 66667) return 2500 + (income - 33333) * 0.25;
        else return 10833 + (income - 66667) * 0.30;
    }

    // ==================================================
    // INPUT VALIDATION
    // ==================================================
    public static int getChoice(Scanner sc, int min, int max) {

        int choice = -1;

        while (choice < min || choice > max) {

            System.out.print("Select option (" + min + "-" + max + "): ");

            if (sc.hasNextInt()) {
                choice = sc.nextInt();
                sc.nextLine();
            } else {
                System.out.println("Invalid input.");
                sc.next();
            }
        }

        return choice;
    }

    // ==================================================
    // DISPLAY PAYROLL
    // ==================================================
    public static void printPayroll(
            String empNum, String name, String birthday,
            int month, double rate,
            double h1, double h2,
            double g1, double g2,
            double sss, double ph, double pagibig,
            double tax, double totalDed,
            double net1, double net2) {

        String[] months = {"", "January", "February", "March", "April", "May",
                "June", "July", "August", "September", "October", "November", "December"};

        System.out.println("\n======================================================");
        System.out.println("Month: " + months[month]);
        System.out.println("======================================================");

        System.out.println("Employee #: " + empNum);
        System.out.println("Employee Name: " + name);
        System.out.println("Birthday: " + birthday);

        System.out.println("\nCutoff Date: " + months[month] + " 1–15");
        System.out.println("Total Hours Worked: " + h1);
        System.out.println("Gross Salary: " + g1);
        System.out.println("Net Salary: " + net1);

        System.out.println("\nCutoff Date: " + months[month] + " 16–End");
        System.out.println("Total Hours Worked: " + h2);
        System.out.println("Gross Salary: " + g2);

        System.out.println("SSS: " + sss);
        System.out.println("PhilHealth: " + ph);
        System.out.println("Pag-IBIG: " + pagibig);
        System.out.println("Tax: " + tax);

        System.out.println("Total Deductions: " + totalDed);
        System.out.println("Net Salary: " + net2);
    }
}
