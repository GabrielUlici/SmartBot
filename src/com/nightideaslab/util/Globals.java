package com.nightideaslab.util;

public class Globals
{
	public int portNC;							/** NaoConsole Port number */
	
	/** Strings for NaoConsole */
	public String urlTurnLeft;					/** String for TurnLeft */
	public String urlForward;					/** String for Forward */
	public String urlTurnRight;					/** String for TurnRight */
	public String urlLeft;						/** String for Left */
	public String urlBackward;					/** String for Backward */
	public String urlRight;						/** String for Right */
	public String urlStartWalk;					/** String for StartWalk */
	public String urlStopWalk;					/** String for StopWalk */
	public String urlSit;						/** String for Sit */
	public String urlSitDown;					/** String for Sit Down */
	public String urlStand;						/** String for Stand */
	public String urlGetUp;						/** String for GetUp Behavior*/
	public String urlGetUpFaceDown;				/** String for GetUp FaceDown */
	public String urlGetUpFaceUp;				/** String for GetUp FaceUp*/
	public String urlForwardLeftKick;			/** String for ForwardLeftKick */
	public String urlForwardRightKick;			/** String for ForwardRightKick */
	public String urlSideLeftKick;				/** String for SideLeftKick */
	public String urlSideRightKick;				/** String for SideRightKick */
	public String urlSaveLeft;					/** String for SaveLeft */
	public String urlBackRightKick;				/** String for BackRightKick */
	public String urlBackLeftKick;				/** String for BackLeftKick */
	public String urlSaveRight;					/** String for SaveRight */
	public String urlHeadLeft;					/** String for HeadLeft */
	public String urlHeadRight;					/** String for HeadRight */
	public String urlHeadUp;					/** String for HeadUp */
	public String urlHeadDown;					/** String for HeadDown */
	public String urlHeadCenter;				/** String for HeadCenter */
	public String urlLArm;						/** String for Left Arm */
	public String urlRArm;						/** String for Right Arm */
	public String urlLElbow;					/** String for Left Elbow */
	public String urlRElbow;					/** String for Right Elbow */
	public String urlHello;						/** String for the Hello animation */
	public String urlHandsUp;					/** String for rising the hands up */
	public String urlHandUp;					/** String for rising the hands up */
	public String urlHandsDown;					/** String for lowering the hands */
	public String urlLeftHandUp;				/** String for rising the hands up */
	public String urlLeftHandDown;				/** String for lowering the hands */
	public String urlRightHandUp;				/** String for rising the hands up */
	public String urlRightHandDown;				/** String for lowering the hands */
	public String urlEyes;						/** String for Eyes animation */
	public String url3MusketeersStory;			/** String for  */
	public String urlCaravanPalaceDance;		/** String for  */
	public String urlFollowMe;					/** String for  */
	public String urlGoAndRest;					/** String for  */
	public String urlPresentation;				/** String for  */
	public String urlStarWarsStory;				/** String for  */
	public String urlThrillerDance;				/** String for  */
	public String urlTaiChiDance;				/** String for the Dance Tai Chi */
	public String urlVangelisDance;				/** String for  */
	public String urlDanceForUs;				/** String for Dance for us */
	public String urlKickForUs;					/** String for Kick for us */
	public String urlDance;						/** String for Do you know how to dance */
	public String urlStory;						/** String for Do you know any story */
	public String urlUnknownCommand;			/** String for Unknown Command */
		
	/** Strings for RAgent2 */
	public String urlSendToRAgent;				/** Property string for RAgent2 */
	public String urlAttacker;					/** Property string for Attacker */
	public String urlFindBall;					/** Property string for FindBall */
	public String urlGoToBall;					/** Property string for GoToBall */
	public String urlDefend;					/** Property string for DefendBall */
	public String urlPenalty;					/** Property string for Penalty */
	public String urlInitial;					/** Property string for Initial */
	
	/** Strings for Joystick */
	public String urlRArrowUp;					/** String for roll */
	public String urlRArrowDown;				/** String for pitch */
	public String urlRArrowLeft;				/** String for XAcc */
	public String urlRArrowRight;				/** String for YAcc */
	public String urlRCenter;					/** String for YAcc */
	public String urlLArrowUp;					/** String for ZAcc */
	public String urlLArrowDown;				/** String for ZAcc */
	public String urlLArrowLeft;				/** String for ZAcc */
	public String urlLArrowRight;				/** String for ZAcc */
	public String urlLCenter;					/** String for ZAcc */
	
	/** Strings for Web HTTP */
	public String urlRunBehavior;					/** String for roll */
	public String urlStopBehavior;				/** String for pitch */
	public String urlGetBehavior;				/** String for YAcc */
	public String urlGetBattery;				/** String for XAcc */
	public String urlEmpty;				/** String for XAcc */
			
	public Globals()
	{
		portNC = 30000;
		
		urlTurnLeft 			= "walk 0 0 0.5";
		urlForward 				= "walk	0.5 0 0";
		urlTurnRight 			= "walk 0 0 -0.5";
		urlLeft 				= "walk 0 0.5 0";
		urlBackward 			= "walk -0.5 0 0";
		urlRight 				= "walk 0 -0.5 0";
		urlStartWalk 			= "walk 0.5 0 0";
		urlStopWalk 			= "stopwalk";
		urlSit 					= "sit";
		urlStand 				= "stand";
		urlGetUpFaceDown 		= "getup FaceDown";
		urlGetUpFaceUp 			= "getup FaceUp";
		urlForwardLeftKick		= "kick ForwardLeft";
		urlForwardRightKick 	= "kick ForwardRight";
		urlSideLeftKick 		= "kick SideLeft";
		urlSideRightKick 		= "kick SideRight";
		urlSaveLeft 			= "save CloseLeft";
		urlBackRightKick 		= "kick BackRight";
		urlBackLeftKick 		= "kick BackLeft";
		urlSaveRight 			= "save CloseRight";
		urlHeadLeft				= "movehead 50 0 0.5";
		urlHeadRight			= "movehead -50 0 0.5";
		urlHeadUp				= "movehead 0 -28 0.5";
		urlHeadDown				= "movehead 0 28 0.5";
		urlHeadCenter			= "movehead 0 0 0.5";
		urlLArm					= "movelarm 0 0 0.5";
		urlRArm					= "moverarm 0 0 0.5";
		urlLElbow				= "movelelbow 0 0 0.5";
		urlRElbow				= "moverelbow 0 0 0.5";
		urlGetUp				= "runbehavior getup";
		urlHello				= "runbehavior hello";
		urlHandsUp				= "handsup";
		urlHandUp				= "handup";
		urlHandsDown			= "handsdown";
		urlLeftHandUp			= "lefthandup";
		urlLeftHandDown			= "lefthanddown";
		urlRightHandUp			= "righthandup";
		urlRightHandDown		= "righthanddown";
		urlSitDown				= "runbehavior sitdown";
		urlEyes					= "blinkeyes 5";
		url3MusketeersStory		= "runbehavior 3_musketeers_story";
		urlCaravanPalaceDance	= "runbehavior caravan_palace_dance";
		urlFollowMe				= "runbehavior follow_me";
		urlGoAndRest			= "runbehavior go_and_rest";
		urlPresentation			= "runbehavior presentation";
		urlStarWarsStory		= "runbehavior starwars_story";
		urlThrillerDance		= "runbehavior thriller_dance";
		urlTaiChiDance			= "runbehavior taichi_dance";
		urlVangelisDance		= "runbehavior vangelis_dance";
		urlDance				= "dance";
		urlDanceForUs			= "danceforus";
		urlKickForUs			= "kickforus";
		urlStory				= "story";
		urlUnknownCommand		= "unknowncommand";
				
		urlSendToRAgent = "sendtoragent";
		urlAttacker 	= "MainAttackerPlan";
		urlFindBall 	= "FindBall";
		urlGoToBall 	= "ApproachBall";
		urlDefend 		= "MainDefenderPlan";
		urlPenalty		= "PenalizedPlan";
		urlInitial		= "InitialPlan";
		
		urlRArrowUp 	= "";
		urlRArrowDown 	= "";
		urlRArrowLeft	= "";
		urlRArrowRight	= "";
		urlRCenter		= "";
		urlLArrowUp		= "";
		urlLArrowDown	= "";
		urlLArrowLeft	= "";
		urlLArrowRight	= "";
		urlLCenter		= "";
		
		/** Strings for Web HTTP */
		urlRunBehavior	= "ALBehaviorManager.runBehavior";
		urlStopBehavior	= "ALBehaviorManager.stopAllBehaviors";
		urlGetBehavior	= "ALBehaviorManager.getInstalledBehaviors";
		urlGetBattery	= "ALSentinel.getBatteryLevel";
		urlEmpty		= "";
			
	}

}
