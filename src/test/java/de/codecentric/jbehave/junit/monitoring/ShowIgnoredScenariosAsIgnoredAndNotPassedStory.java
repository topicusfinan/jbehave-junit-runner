package de.codecentric.jbehave.junit.monitoring;

import java.util.Collections;

import de.codecentric.jbehave.junit.monitoring.step.ExampleSteps;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.junit.JUnitStory;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.junit.runner.RunWith;

@RunWith(JUnitReportingRunner.class)
public class ShowIgnoredScenariosAsIgnoredAndNotPassedStory extends JUnitStory
{

	public ShowIgnoredScenariosAsIgnoredAndNotPassedStory()
	{
		JUnitReportingRunner.recommendedControls(configuredEmbedder());
		configuredEmbedder().useMetaFilters(Collections.singletonList("-skip"));
	}

	@Override
	public InjectableStepsFactory stepsFactory()
	{
		return new InstanceStepsFactory(configuration(), new ExampleSteps());
	}

	@Override
	public Configuration configuration()
	{
		// add custom coverters
		return new MostUsefulConfiguration();
	}

}
