package de.codecentric.jbehave.junit.monitoring;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.Keywords;
import org.jbehave.core.model.ExamplesTable;
import org.jbehave.core.model.GivenStories;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;
import org.jbehave.core.steps.CandidateSteps;
import org.jbehave.core.steps.StepCandidate;
import org.jbehave.core.steps.StepType;
import org.jbehave.core.steps.Steps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class JUnitDescriptionGeneratorTest
{

	private static final String DEFAULT_STORY_NAME = "Default Story Name";
	private static final String DEFAULT_SCENARIO_TITLE = "Default Scenario Title";
	@Mock
	StepCandidate stepCandidate;
	@Mock
	Steps steps;
	@Mock
	Story story;
	@Mock
	Scenario scenario;
	@Mock
	GivenStories givenStories;
	@Mock
	Configuration configuration;

	private JUnitDescriptionGenerator generator;
	private Description description;

	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);
		when(steps.listCandidates()).thenReturn(Collections.singletonList(stepCandidate));
		when(stepCandidate.matches(anyString())).thenReturn(true);
		when(stepCandidate.matches(anyString(), anyString())).thenReturn(true);
		when(stepCandidate.getStepsType()).then(returnObjectClass());
		when(story.getName()).thenReturn(DEFAULT_STORY_NAME);
		when(scenario.getTitle()).thenReturn(DEFAULT_SCENARIO_TITLE);
		when(givenStories.getPaths()).thenReturn(Collections.<String> emptyList());
		when(scenario.getGivenStories()).thenReturn(givenStories);
		when(configuration.keywords()).thenReturn(new Keywords());
		generator = new JUnitDescriptionGenerator(Arrays.asList(new CandidateSteps[] { steps }), configuration);
	}

	private Answer<Class<?>> returnObjectClass()
	{
		return new Answer<Class<?>>()
		{

			public Class<?> answer(InvocationOnMock invocation) throws Throwable
			{
				return Object.class;
			}
		};
	}

	@Test
	public void shouldCountSteps()
	{
		addScenarioToStory(scenario);
		addStepToScenario();
		generateStoryDescription();
		assertThat(generator.getTestCases(), is(1));
	}

	@Test
	public void shouldCountIgnorables()
	{
		when(scenario.getSteps()).thenReturn(Arrays.asList("Given Step1", "!-- ignore me"));
		when(stepCandidate.matches(anyString(), anyString())).thenReturn(false);
		when(stepCandidate.matches(eq("Given Step1"), anyString())).thenReturn(true);
		generator.createDescriptionFrom(scenario);
		assertThat(generator.getTestCases(), is(2));
	}

	@Test
	public void shouldGenerateDescriptionForTopLevelScenario()
	{
		when(scenario.getTitle()).thenReturn("MyTitle");
		generateScenarioDescription();
		assertThat(description, equalTo(Description.createSuiteDescription("Scenario: MyTitle")));
		assertThat(generator.getTestCases(), is(0));
	}

	@Test
	public void shouldGenerateDescriptionForStep()
	{
		addStepToScenario();
		generateScenarioDescription();
		assertThat(description.getChildren(), hasItem(step1Description()));
		assertThat(generator.getTestCases(), is(1));
	}

	@Test
	public void shouldGenerateDescriptionForTabularParameterStep()
	{
		when(scenario.getSteps()).thenReturn(Collections.singletonList("StepWithTableParam:\n|Head|\n|Value|"));
		generateScenarioDescription();
		assertThat(description.getChildren(), hasItem(stepWithTableDescription()));
		assertThat(generator.getTestCases(), is(1));
	}

	@Test
	public void shouldGenerateDescriptionForStory()
	{
		generateStoryDescription();
		assertThat(description, is(Description.createSuiteDescription(DEFAULT_STORY_NAME)));
	}

	@Test
	public void shouldGenerateDescriptionForScenarioChildOfStory()
	{
		addScenarioToStory(scenario);
		generateStoryDescription();
		assertThat(description.getChildren(), hasItem(Description.createSuiteDescription("Scenario: " + DEFAULT_SCENARIO_TITLE)));
	}

	@Test
	public void shouldStripLinebreaksFromScenarioDescriptions()
	{
		Scenario scenario = mock(Scenario.class);
		addScenarioToStory(scenario);
		when(scenario.getGivenStories()).thenReturn(givenStories);

		when(scenario.getTitle()).thenReturn("Scenario with\nNewline");
		generateStoryDescription();
		assertThat(firstChild(description).getDisplayName(), not(containsString("\n")));
	}

	private void addScenarioToStory(Scenario... scenario)
	{
		when(story.getScenarios()).thenReturn(Arrays.asList(scenario));
	}

	@Test
	public void shouldStripCarriageReturnsFromScenarioDescriptions()
	{
		Scenario scenario = mock(Scenario.class);
		addScenarioToStory(scenario);
		when(scenario.getGivenStories()).thenReturn(givenStories);

		when(scenario.getTitle()).thenReturn("Scenario with\rCarriage Return");
		generateStoryDescription();
		assertThat(firstChild(description).getDisplayName(), not(containsString("\r")));
	}

	@Test
	public void shouldCopeWithSeeminglyDuplicateSteps() throws Exception
	{
		when(scenario.getSteps()).thenReturn(Arrays.asList("Given Step1", "Given Step1"));
		generateScenarioDescription();
		assertThat(description.getChildren(), everyItem(whoseDisplayName(startsWith("Given Step1"))));
		assertThat(description.getChildren().size(), is(2));
		assertThat(description.getChildren(), allChildrenHaveUniqueDisplayNames());
		assertThat(generator.getTestCases(), is(2));
	}

	@Test
	public void shouldCopeWithDuplicateGivenStories() throws Exception
	{
		addScenarioToStory(scenario, scenario);
		when(givenStories.getPaths()).thenReturn(Arrays.asList("/some/path/to/GivenStory.story"));
		generateStoryDescription();
		Description firstScenario = firstChild(description);
		Description secondScenario = description.getChildren().get(1);
		assertThat(firstChild(firstScenario).getDisplayName(), is(not(firstChild(secondScenario).getDisplayName())));
	}

	@Test
	public void shouldGenerateDescriptionForGivenStories()
	{
		when(givenStories.getPaths()).thenReturn(Arrays.asList("/some/path/to/GivenStory.story"));
		generateScenarioDescription();
		assertThat(firstChild(description), hasProperty("displayName", is("GivenStory.story")));
		assertThat(generator.getTestCases(), is(1));
	}

	@Test
	public void shouldGenerateDescriptionForExampleTablesOnScenario()
	{
		addStepToScenario();
		int NUM_ROWS = 2;
		Map<String, String> row = addExamplesTableToScenario(NUM_ROWS);

		generateScenarioDescription();

		assertThat(description.getChildren().size(), is(NUM_ROWS));
		for (Description exampleDescription : description.getChildren())
		{
			assertThat(exampleDescription.getChildren(), hasItem(Matchers.<Description> hasProperty("displayName", startsWith("Given Step1"))));
			assertThat(exampleDescription, hasProperty("displayName", startsWith("Example: " + row)));
		}
	}

	@Test
	public void shouldGenerateChildrenForComposedSteps()
	{
		addStepToScenario();
		when(stepCandidate.composedSteps()).thenReturn(new String[] { "compositeStep1", "compositeStep2" });
		StepCandidate composedStep1 = stepCandidateMock("compositeStep1");
		StepCandidate composedStep2 = stepCandidateMock("compositeStep2");
		when(stepCandidate.matches(anyString(), anyString())).thenReturn(false);
		when(stepCandidate.matches(eq("Given Step1"), anyString())).thenReturn(true);
		when(stepCandidate.isComposite()).thenReturn(true);
		when(steps.listCandidates()).thenReturn(Arrays.asList(stepCandidate, composedStep1, composedStep2));
		generator = new JUnitDescriptionGenerator(Arrays.asList(new CandidateSteps[] { steps }), configuration);

		generateScenarioDescription();

		Description composedStep = firstChild(description);
		verify(stepCandidate, times(0)).getStepsInstance();
		assertThat(composedStep.getChildren(), everyItem(Matchers.<Description> hasProperty("displayName", startsWith("compositeStep"))));
		assertThat(composedStep.getChildren().size(), is(2));
		assertThat(composedStep.isSuite(), is(true));
		assertThat(composedStep.getDisplayName(), startsWith("Given Step1"));
		assertThat(generator.getTestCases(), is(2));
	}

	@Test
	public void shouldCreateDescriptionForAndStep()
	{
		when(scenario.getSteps()).thenReturn(Arrays.asList("Given Step1", "And Step2"));
		when(stepCandidate.matches(anyString(), anyString())).thenReturn(false);
		when(stepCandidate.matches(eq("Given Step1"), anyString())).thenReturn(true);
		when(stepCandidate.matches(eq("And Step2"), eq(StepType.GIVEN.toString() + " "))).thenReturn(true);
		when(stepCandidate.getStepType()).thenReturn(StepType.GIVEN);
		when(stepCandidate.getStartingWord()).thenReturn("GIVEN");
		generateScenarioDescription();
		assertThat(description.getChildren().size(), is(2));
	}

	@Test
	public void shouldGenerateDescriptionForPendingSteps()
	{
		addStepToScenario();
		when(steps.listCandidates()).thenReturn(Arrays.asList(new StepCandidate[] { null }));
		when(stepCandidate.matches(anyString())).thenReturn(false);
		when(stepCandidate.matches(anyString(), anyString())).thenReturn(false);
		generateScenarioDescription();
		assertThat(description.getChildren().size(), is(1));
		assertThat(description.getChildren(), everyItem(Matchers.<Description> hasProperty("displayName", containsString("PENDING"))));
		assertThat(generator.getTestCases(), is(1));
	}

	@Test
	public void shouldGenerateDescriptionForIgnorableSteps()
	{
		when(scenario.getSteps()).thenReturn(Collections.singletonList("!-- Comment"));

		when(stepCandidate.matches(anyString(), anyString())).thenReturn(false);
		when(stepCandidate.matches(eq("!-- Comment"), eq(StepType.IGNORABLE.toString() + " "))).thenReturn(true);
		when(stepCandidate.getStepType()).thenReturn(StepType.IGNORABLE);
		when(stepCandidate.getStartingWord()).thenReturn("!--");
		generateScenarioDescription();
		assertThat(description.getChildren().size(), is(1));

	}

	@Test
	public void shouldSkipExampleTablesForParameterizedGivenStories()
	{
		// jbehave ignores example tables, when given stories are parameterized
		addStepToScenario();
		when(givenStories.getPaths()).thenReturn(Collections.singletonList("/some/path/to/GivenStory.story#{0}"));
		when(givenStories.requireParameters()).thenReturn(true);

		generateScenarioDescription();

		assertThat(firstChild(description), hasProperty("displayName", is("GivenStory.story")));
		assertThat(generator.getTestCases(), is(2));

		assertThat(description.getChildren().size(), is(2));
		for (Description exampleDescription : description.getChildren())
		{
			assertThat(exampleDescription.getChildren().size(), is(0));
		}

	}

	private void generateScenarioDescription()
	{
		description = generator.createDescriptionFrom(scenario);
	}

	private void generateStoryDescription()
	{
		description = generator.createDescriptionFrom(story);
	}

	private Matcher<Description> whoseDisplayName(Matcher<String> startsWith)
	{
		return Matchers.hasProperty("displayName", startsWith);
	}

	private StepCandidate stepCandidateMock(String name)
	{
		StepCandidate step = mock(StepCandidate.class);
		when(step.getStepsType()).then(returnObjectClass());
		when(step.matches(eq(name), anyString())).thenReturn(true);
		return step;
	}

	private void addStepToScenario()
	{
		when(scenario.getSteps()).thenReturn(Collections.singletonList("Given Step1"));
	}

	private Description step1Description()
	{
		return Description.createTestDescription(Object.class, "Given Step1");
	}

	private Description stepWithTableDescription()
	{
		return Description.createTestDescription(Object.class, "StepWithTableParam:");
	}

	private Map<String, String> addExamplesTableToScenario(int NUM_ROWS)
	{
		ExamplesTable examplesTable = mock(ExamplesTable.class);
		when(examplesTable.getRowCount()).thenReturn(NUM_ROWS);
		Map<String, String> row = new TreeMap<>();
		List<Map<String, String>> rows = new ArrayList<>();
		for (int i = 1; i <= 10; i++)
		{
			row.put("key" + i, "value" + i);
		}
		for (int i = 1; i <= NUM_ROWS; i++)
		{
			rows.add(row);
		}
		when(examplesTable.getRow(anyInt())).thenReturn(row);
		when(examplesTable.getRows()).thenReturn(rows);
		when(scenario.getExamplesTable()).thenReturn(examplesTable);
		return row;
	}

	private Description firstChild(Description description)
	{
		return description.getChildren().get(0);
	}

	private Matcher<List<Description>> allChildrenHaveUniqueDisplayNames()
	{
		return new BaseMatcher<List<Description>>()
		{

			private List<Description> descriptions;

			@SuppressWarnings("unchecked")
			public boolean matches(Object descriptions)
			{
				this.descriptions = (List<Description>) descriptions;
				Set<String> displayNames = new HashSet<>();
				for (Description child : this.descriptions)
				{
					displayNames.add(child.getDisplayName());
				}
				return displayNames.size() == this.descriptions.size();
			}

			public void describeTo(org.hamcrest.Description description)
			{
				description.appendText("Children of description do not have unique display names");
				for (Description child : descriptions)
				{
					description.appendText(child.getDisplayName());
				}
			}

		};
	}
}
