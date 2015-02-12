package il.technion.shades.kbr;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class Statistics {
	
	public AtomicInteger nrHandledMsgs;
	public AtomicInteger nrFirstSideSteps;
	public AtomicInteger nrSecondSideSteps;
	public AtomicInteger nrFirstSideStepHits;
	public AtomicInteger nrFirstSideStepMisses;
	public AtomicInteger nrSecondSideStepHits;
	public AtomicInteger nrSecondSideStepMisses;
	public AtomicInteger nrMsgsBeforeFirstSideStep;
	public AtomicInteger nrMsgsBeforeSecondSideStep;
	public AtomicInteger nrColorNodeFound;
	public AtomicInteger nrNoColorNodeFound; 

	@Inject
	public Statistics(
			@Named("openkad.testing.nrIncomingMessages") AtomicInteger nrHandledMsgs,
			@Named("openkad.shades.nrFirstSideSteps") AtomicInteger nrFirstSideSteps,
			@Named("openkad.shades.nrSecondSideSteps") AtomicInteger nrSecondSideSteps,
			@Named("openkad.shades.nrFirstSideStepHits") AtomicInteger nrFirstSideStepHits,
			@Named("openkad.shades.nrFirstSideStepMisses") AtomicInteger nrFirstSideStepMisses,
			@Named("openkad.shades.nrSecondSideStepHits") AtomicInteger nrSecondSideStepHits,
			@Named("openkad.shades.nrSecondSideStepMisses") AtomicInteger nrSecondSideStepMisses,
			@Named("openkad.shades.nrMsgsBeforeFirstSideStep") AtomicInteger nrMsgsBeforeFirstSideStep,
			@Named("openkad.shades.nrMsgsBeforeSecondSideStep") AtomicInteger nrMsgsBeforeSecondSideStep,
			@Named("openkad.shades.nrColorNodeFound") AtomicInteger nrColorNodeFound,
			@Named("openkad.shades.nrNoColorNodeFound") AtomicInteger nrNoColorNodeFound){
		this.nrHandledMsgs = nrHandledMsgs;
		this.nrFirstSideSteps = nrFirstSideSteps;
		this.nrSecondSideSteps = nrSecondSideSteps;
		this.nrFirstSideStepHits = nrFirstSideStepHits;
		this.nrFirstSideStepMisses = nrFirstSideStepMisses;
		this.nrSecondSideStepHits = nrSecondSideStepHits;
		this.nrSecondSideStepMisses = nrSecondSideStepMisses;
		this.nrMsgsBeforeFirstSideStep = nrMsgsBeforeFirstSideStep;
		this.nrMsgsBeforeSecondSideStep = nrMsgsBeforeSecondSideStep;
		this.nrColorNodeFound = nrColorNodeFound;
		this.nrNoColorNodeFound = nrNoColorNodeFound;
	}
	
}
