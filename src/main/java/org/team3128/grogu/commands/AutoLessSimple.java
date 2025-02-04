package org.team3128.grogu.commands;

import org.team3128.common.drive.DriveCommandRunning;
import org.team3128.common.hardware.limelight.Limelight;
import org.team3128.grogu.subsystems.FalconDrive;
import org.team3128.grogu.subsystems.PathFinding;
import org.team3128.grogu.subsystems.*;
import edu.wpi.first.wpilibj2.command.*;

import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;

public class AutoLessSimple extends SequentialCommandGroup {

    public AutoLessSimple(Limelight shooterLimelight, DriveCommandRunning cmdRunning, double goalHorizontalOffset, PathFinding m_robotContainer, FalconDrive mRobotDrive, Hopper hopper) {       
        addCommands(
            new CmdAlignShoot(shooterLimelight, cmdRunning, goalHorizontalOffset, 3),
            new InstantCommand(() -> hopper.runIntake()),
            m_robotContainer.getAutonomousCommandLessSimple(mRobotDrive),
            new InstantCommand(() -> hopper.stopIntake())
            );
    }
}