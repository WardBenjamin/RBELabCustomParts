import com.neuronrobotics.bowlerstudio.vitamins.Vitamins;
CSGDatabase.clear()
class ServoBoxMaker{
	public List<CSG> make(){
		double vexGrid = (1.0/2.0)* 25.4
		double shellThickness =2 
		String servoType = "hv5932mg"
		LengthParameter printerOffset 		= new LengthParameter("printerOffset",0.5,[1.2,0])                
		LengthParameter boltLength		= new LengthParameter("Bolt Length",10,[180,10])
		LengthParameter servoAngle		= new LengthParameter("Servo angle",0,[90,-90])
		StringParameter bearingSizeParam 			= new StringParameter("Bearing Size","R8-60355K505",Vitamins.listVitaminSizes("ballBearing"))
		StringParameter servoparam 			= new StringParameter("Servo to use",servoType,Vitamins.listVitaminSizes("hobbyServo"))
		StringParameter hornparam 			= new StringParameter("Horn type","hv6214mg_6",Vitamins.listVitaminSizes("hobbyServoHorn"))
		StringParameter gearAParam 			 	= new StringParameter("Gear A","36T",Vitamins.listVitaminSizes("vexGear"))
		
		HashMap<String, Object>  gearAMeasurments = Vitamins.getConfiguration( "vexGear",gearAParam.getStrValue())
		HashMap<String, Object>  bearingData = Vitamins.getConfiguration("ballBearing",bearingSizeParam.getStrValue())
		HashMap<String, Object>  vitaminData = Vitamins.getConfiguration( "hobbyServo",servoparam.getStrValue())
		
		double washerThickness = 1
		
		CSG gearA = Vitamins.get( "vexGear",gearAParam.getStrValue())
		CSG vitaminFromScript = Vitamins.get("hobbyServo",servoparam.getStrValue())
							.rotz(servoAngle.getMM())
		CSG horn = Vitamins.get("hobbyServoHorn",hornparam.getStrValue())
						.toolOffset(printerOffset.getMM())
						.rotx(180)
						.movez(vitaminFromScript.getMaxZ())
		double bearingSurface = horn.getMaxZ()+2
		CSG vshaft =  (CSG)ScriptingEngine
			                    .gitScriptRun(
		                                "https://github.com/WPIRoboticsEngineering/RBELabCustomParts.git", // git location of the library
			                              "vexShaft.groovy" , // file to load
			                              [vitaminFromScript.getMaxZ()*2]
		                        )
		               .movez(bearingSurface)
		boltLength.setMM(100)                       
		CSG bolt = Vitamins.get("capScrew","8#32").toolOffset(printerOffset.getMM()).rotz(45)
		CSG bearing = Vitamins.get("ballBearing",bearingSizeParam.getStrValue())
				.makeKeepaway(printerOffset.getMM()/2)
				.toZMin()
				.movez(bearingSurface+washerThickness+shellThickness)
				
	
		double pinRadius =bearingData.innerDiameter/2
		CSG pin =new Cylinder(pinRadius,bearingData.width+shellThickness+washerThickness-printerOffset.getMM()*2).toCSG() // a one line Cylinder
					.movez(bearingSurface)
		double washerRadius = bearingData.innerDiameter/2-(printerOffset.getMM()/2)+2
		CSG washer =new Cylinder(washerRadius,washerThickness+shellThickness*2-printerOffset.getMM()).toCSG() // a one line Cylinder
					.movez(bearingSurface-shellThickness)
		CSG boltHead =new Cylinder(3,3).toCSG() // a one line Cylinder
		CSG flange =new Cylinder(washerRadius+1,washerThickness).toCSG() // a one line Cylinder
					.toZMax()
					.movez(bearingSurface-shellThickness)	
		CSG boltHeadKeepaway =new Cylinder(3,3).toCSG() // a one line Cylinder
						.toZMin()	
						.movez(flange.getMinZ())		
		double topShellTHickness =bearingData.width+shellThickness
		CSG bearingLug=new Cylinder(bearingData.outerDiameter/2+shellThickness,topShellTHickness).toCSG() // a one line Cylinder
					.movez(bearingSurface+washerThickness)
		CSG boltLug=new Cylinder(2+shellThickness,topShellTHickness).toCSG()  	
				.movez(bearingSurface+washerThickness)
		
		boltLug=CSG.unionAll([boltLug,
						boltLug.movey(vexGrid),
						boltLug.movey(-vexGrid)])
		boltLug=CSG.unionAll([boltLug.movex(vexGrid*2),
						boltLug.movex(-vexGrid*2)])
						.hull()
		double gridOffset = 2
		CSG bolts = CSG.unionAll([bolt.movex(vexGrid*gridOffset),
							bolt.movex(-vexGrid*gridOffset)])
		bolts=CSG.unionAll([bolts,
						bolts.movey(vexGrid),
						bolts.movey(-vexGrid)])
					.movez(bearingSurface+10)		
		double bottomOfGear = vitaminData.bottomOfFlangeToTopOfBody+1
		
		CSG pinSection =CSG.unionAll([washer,pin,flange])
						.difference(bearing.movez(-printerOffset.getMM()*2))
		
		pinSection=pinSection
			.intersect(
				pinSection
				.getBoundingBox()
				.movey(-pinRadius*0.75)
				)
			.intersect(
				pinSection
				.getBoundingBox()
				.movey(pinRadius*0.75)
				)
			
		CSG cutter =pinSection.toolOffset(printerOffset.getMM())
		
		gearA=gearA
			.toZMax()
			.movez(bearingSurface)
			.union(gearA
					.toZMin()
					.movez(bottomOfGear))
			.difference(horn.movez(-washerThickness))
		double gearThickness = gearA.getTotalZ()
		
		for(double i=0;i<gearThickness+washerThickness*2;i+=washerThickness){
			gearA=gearA.difference(cutter.movez(-i))
				
		}
		
		pinSection=pinSection.difference(vshaft)
					.difference(boltHeadKeepaway)
		CSG allignment = Vitamins.get("vexFlatSheet","Aluminum 5x15")	
						.rotz(90)
						
		allignment=allignment
					.movey(vexGrid*3)	
					.movex(-vexGrid*2)	
					.movez(bearingLug.getMaxZ()-printerOffset.getMM())
					
		CSG caseTop = bearingLug
					.union(boltLug)
					.difference(pin.hull().makeKeepaway(shellThickness*3).movez(-printerOffset.getMM()*2))
					.difference(bearing.movez(-printerOffset.getMM()*2).hull())
					.difference(bearing.movez(printerOffset.getMM()).hull())
					.difference(bolts)
					.minkowskiDifference(allignment,printerOffset.getMM())
		CSG gearKeepaway = CSG.unionAll([gearA.hull().makeKeepaway(1),
									gearA.getBoundingBox().toolOffset(2).movez(washerThickness),
									gearA.getBoundingBox().toolOffset(2).movez(-washerThickness)
						])
		CSG caseBottom = boltLug.toZMax()
						.movez(boltLug.getMinZ())
						.difference(gearKeepaway)
						.difference(bolts)
		// println vitaminData
		double servomountTHickness = vitaminData.bottomOfFlangeToTopOfBody-vitaminData.flangeThickness
		CSG servoMpunt =new Cube(vitaminData.servoThinDimentionThickness,
						vitaminData.flangeLongDimention,
						servomountTHickness).toCSG()
						.toYMax()
						.movey(vitaminData.shaftToShortSideDistance+(vitaminData.flangeLongDimention-vitaminData.servoThickDimentionThickness)/2)
						.toZMin()
						.movez(vitaminData.flangeThickness)
						.rotz(servoAngle.getMM())
		CSG boltLugLower=new Cylinder(2+shellThickness,servomountTHickness).toCSG()  	
				.movez(vitaminData.flangeThickness)
		
		boltLugLower=CSG.unionAll([boltLugLower,
						boltLugLower.movey(vexGrid),
						boltLugLower.movey(-vexGrid)])
		boltLugLower=CSG.unionAll([boltLugLower.movex(vexGrid*2),
						boltLugLower.movex(-vexGrid*2)])
						.hull()
		servoMpunt=servoMpunt.union(	boltLugLower).hull()	
						
		caseBottom=CSG.unionAll([caseBottom,
							servoMpunt,
		caseBottom.toZMin().movez(vitaminData.bottomOfFlangeToTopOfBody),
		caseBottom.toZMin().movez(vitaminData.flangeThickness)
		]).hull()
		.difference(gearKeepaway)
		.minkowskiDifference(vitaminFromScript,printerOffset.getMM())
		.difference(bolts)
		
		pinSection.setMfg({toMfg ->
			return toMfg.rotx(90)
				.toZMin()
		})
		
		gearA.setMfg({toMfg ->
			return toMfg.rotx(180)
				.toZMin()
		})
		caseTop.setMfg({toMfg ->
			return toMfg
				.toZMin()
		})
		caseBottom.setMfg({toMfg ->
			return toMfg
				.toZMin()
		})
		
		gearA.setName("GearModule")
		caseTop.setName("caseTop")
		caseBottom.setName("caseBottom")
		pinSection.setName("pinSection")
		def parts= [gearA,pinSection,caseTop,caseBottom]
		//println "Preping "+parts.size()
		for(int i=0;i<parts.size();i++){
			//println parts.get(i).getName()
			int index = i
			parts.get(i).setRegenerate({make()[index]})
			parts.get(i).getMapOfparametrics().clear()
			parts.get(i).setParameter(servoparam)
			parts.get(i).setParameter(hornparam)
			parts.get(i).setParameter(bearingSizeParam)
			parts.get(i).setParameter(servoAngle)
			parts.get(i).setParameter(printerOffset)
		}
		return parts
	}
}

return new ServoBoxMaker().make()