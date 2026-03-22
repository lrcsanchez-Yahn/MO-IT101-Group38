# Changelog

All notable changes to the MotorPH Payroll System project are documented in this file.

This project implements a payroll processing system that reads employee and attendance data from CSV files and computes salaries and government deductions.

---

## Version 1.1 – Code Improvements (TA Feedback Revision)

### Documentation
- Added program-level documentation describing the system.
- Added detailed comments explaining the logic of major functions.
- Documented computation methods such as:
  - `computeHours`
  - `computeSSS`
  - `computePhilHealth`
  - `computePagibig`
  - `computeTax`

### Modularity Improvements
- Refactored the program to separate logic into reusable functions:
  - `handleEmployeeView`
  - `handlePayrollMenu`
  - `processPayroll`
  - `loadAttendance`
  - `computeMonthlyPayroll`
- Simplified the `main()` method so it only controls program flow.

### Performance Improvements
- Optimized file handling by loading `attendance.csv` once and storing the records in memory.
- Removed repeated file opening inside employee loops.

### Algorithm Fixes
- Updated `computeHours()` to:
  - Enforce working hours between **8:00 AM – 5:00 PM**
  - Apply the **grace period rule**
  - Deduct the required **1-hour lunch break**

### Code Quality Improvements
- Replaced magic numbers with constants:
  - `START_WORK`
  - `END_WORK`
  - `LUNCH_BREAK`
- Improved variable naming for better readability.
- Added explanations inside conditional logic.

### Future-Proofing
- Removed hardcoded **June–December restriction**.
- Payroll processing now dynamically handles any month present in the attendance dataset.

---

## Version 1.0 – Initial Implementation

### Features
- Login system for employee and payroll staff.
- Employee information lookup.
- Payroll computation based on:
  - Hourly rate
  - Attendance records
- Deduction calculations for:
  - SSS
  - PhilHealth
  - Pag-IBIG
  - Withholding Tax
- Payroll summary display for each cutoff period.

---
