package org.team3128.grogu.subsystems;

import org.team3128.common.utility.Log;
import org.team3128.common.utility.test_suite.CanDevices;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.revrobotics.CANEncoder;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

//import org.team3128.testbench.subsystems.Constants;
import org.team3128.common.hardware.motor.LazyCANSparkMax;
import org.team3128.common.hardware.motor.LazyTalonFX;
import org.team3128.common.hardware.motor.LazyTalonSRX;
import org.team3128.common.hardware.motor.LazyVictorSPX;

import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.PIDSubsystem;
import edu.wpi.first.wpilibj.controller.PIDController;


public class Shooter extends PIDSubsystem {
    public enum ShooterState {
        OFF(0),
        LONG_RANGE(5500), // long range shooting
        MID_RANGE(2000), //4800 actual setpoint  // mid range shooting
        SHORT_RANGE(3500),
        GREEN(1200),
        YELLOW(5000),
        BLUE(3330),
        RED(3333),
        ; // short range shooting 3700

        public double shooterRPM;

        private ShooterState(double RPM) {
            this.shooterRPM = RPM;
        }
    }

    public static final Shooter instance = new Shooter();
    public static LazyTalonFX LEFT_SHOOTER;
    public static LazyTalonFX RIGHT_SHOOTER;

    public static boolean DEBUG = true;
    double current = 0;
    double error = 0;
    public double output = 0;
    double accumulator = 0;
    double prevError = 0;
    public boolean isAligned = false;

    double value = 0, preValue = 0, time = 0, preTime = 0;

    int plateauCount = 0;

    // private StateTracker stateTracker = StateTracker.getInstance();
    public ShooterState SHOOTER_STATE = ShooterState.MID_RANGE;

    private Shooter() {

        super(new PIDController(Constants.ShooterConstants.SHOOTER_PID.kP, Constants.ShooterConstants.SHOOTER_PID.kI, Constants.ShooterConstants.SHOOTER_PID.kD));
        getController().setTolerance(Constants.ShooterConstants.RPM_THRESHOLD);
        //.setDistancePerPulse(ShooterConstants.kEncoderDistancePerPulse);


        configMotors();
        configEncoders();
        setSetpoint(0);
    }

    public boolean atSetpoint() {
        return m_controller.atSetpoint();
    }

    public boolean isPlateaued() {
        return (plateauCount >= Constants.ShooterConstants.PLATEAU_COUNT);
    }

    private void configMotors() {
        LEFT_SHOOTER = new LazyTalonFX(Constants.ShooterConstants.SHOOTER_MOTOR_LEFT_ID);
        RIGHT_SHOOTER = new LazyTalonFX(Constants.ShooterConstants.SHOOTER_MOTOR_RIGHT_ID);
        if (DEBUG) {
            Log.info("Shooter", "Config motors");
        }
    }

    private void configEncoders() {
        // SHOOTER_ENCODER = LEFT_SHOOTER.getEncoder();
        // if (DEBUG) {
        //     Log.info("Shooter", "Config encoders");
        // }
    }

    public static Shooter getInstance() {
        return instance;
    }

    @Override
    public double getMeasurement() {
        //Log.info("shooter", "getting measurement");
        return LEFT_SHOOTER.getSelectedSensorVelocity(0) * 10 * 60 / Constants.MechanismConstants.ENCODER_RESOLUTION_PER_ROTATION;
    }

    @Override
    public void useOutput(double output, double setpoint) {
        double voltageOutput = shooterFeedForward(setpoint) + output;
        double voltage = RobotController.getBatteryVoltage(); // TODO: investigate bus voltage

        output = voltageOutput / 12;//voltage

        //Log.info("Shooter", "using output");

        value = getMeasurement();
        time = RobotController.getFPGATime() / 1e6;
        
        double accel = (value - preValue) / (time - preTime);

        Log.info("Shooter",getMeasurement()+" RPM");

        if ((Math.abs(value - preValue) <= Constants.ShooterConstants.RPM_PLATEAU_THRESHOLD) &&(Math.abs(value - setpoint) <= Constants.ShooterConstants.RPM_THRESHOLD) && (setpoint != 0)) {
            plateauCount++;
        } else {
            plateauCount = 0;
        }

        preValue = value;
        preTime = time;

        if (output > 1) {
            // Log.info("SHOOTER",
            // "WARNING: Tried to set power above available voltage! Saturation limit SHOULD
            // take care of this ");
            output = 1;
        } else if (output < -1) {
            // Log.info("SHOOTER",
            // "WARNING: Tried to set power above available voltage! Saturation limit SHOULD
            // take care of this ");
            output = -1;
        }

        if(setpoint == 0) {
            output = 0;
        }

        // if (accel > Constants.SHOOTER_MAX_ACCELERATION)
        //     output = output / (accel / Constants.SHOOTER_MAX_ACCELERATION);

        // if (setpoint != 0) {
        //     Log.info("Shooter", "shooter setpoint = " + setpoint);
        // }

        LEFT_SHOOTER.set(ControlMode.PercentOutput, output);
        RIGHT_SHOOTER.set(ControlMode.PercentOutput, -output);
    }

    /*public void setSetpoint(double passedSetpoint) {
        plateauCount = 0;
        super.setSetpoint(passedSetpoint);
        //Log.info("Shooter", "Set setpoint to" + String.valueOf(setpoint));
    }*/

    public void setState(ShooterState shooterState) {
        SHOOTER_STATE = shooterState;
        //setSetpoint(shooterState.shooterRPM);
    }

    public void shoot() {
        setSetpoint(SHOOTER_STATE.shooterRPM);
    }

    public void counterShoot() {
        setSetpoint(0);
    }

    //@Override
    //public void periodic() {
        //Log.info("Shooter", "running periodic");
       
    //}

    public double shooterFeedForward(double desiredSetpoint) {
        //double ff = (0.00211 * desiredSetpoint) - 2; // 0.051
        // feed forward: 0.0019
        double ff = ((0.0019) * desiredSetpoint);//0.00168//0.00170 // 0.00188*x //0.00147x - 0.2; // 0
        if (getSetpoint() != 0) {
            return ff;
        } else {
            return 0;
        }
    }

    // public double getRPMFromDistance() {
    //     return stateTracker.getState().targetShooterState.shooterRPM;
    // }

    // public boolean isReady() {
    //     return (plateauCount > Constants.PLATEAU_COUNT);
    // }

    // public void queue(){
    //     setState(stateTracker.getState().targetShooterState);
    // }

    public ShooterState getState() {
        return SHOOTER_STATE;
    }

    public boolean isReady() {
        if (atSetpoint())
            Log.info("Shooter","at Setpoint");
        if (isAligned)
            Log.info("Shooter","is Aligned");
        return ((isAligned || SHOOTER_STATE == ShooterState.GREEN) && isPlateaued());
        //return true;
    }
}
