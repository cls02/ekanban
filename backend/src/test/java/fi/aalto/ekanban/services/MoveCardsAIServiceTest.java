package fi.aalto.ekanban.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

import static fi.aalto.ekanban.ApplicationConstants.ANALYSIS_PHASE_ID;
import static fi.aalto.ekanban.ApplicationConstants.DEVELOPMENT_PHASE_ID;
import static fi.aalto.ekanban.ApplicationConstants.TEST_PHASE_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import fi.aalto.ekanban.builders.CardBuilder;
import fi.aalto.ekanban.builders.CardPhasePointBuilder;
import fi.aalto.ekanban.models.db.games.Card;
import fi.aalto.ekanban.models.db.games.CardPhasePoint;
import fi.aalto.ekanban.models.db.phases.Phase;
import fi.aalto.ekanban.models.MoveCardAction;
import fi.aalto.ekanban.utils.TestGameContainer;
import fi.aalto.ekanban.services.ai.movecards.MoveCardsAIService;

@RunWith(HierarchicalContextRunner.class)
public class MoveCardsAIServiceTest {
    private static MoveCardsAIService moveCardsAIService;
    private TestGameContainer initialGameContainer;
    private List<MoveCardAction> generatedActions;
    private List<String> toColumnIds;

    @BeforeClass
    public static void initService() {
        moveCardsAIService = new MoveCardsAIService();
    }

    @Before
    public void init() {
        initialGameContainer = TestGameContainer.withNormalDifficultyMockGame();
    }

    public class withInProgressCards {
        @Before
        public void initAndDoAction() {
            initialGameContainer.fillFirstWorkPhasesWithSomeCardsInProgress();
            initialGameContainer.fillLastWorkPhaseWithSomeCardsInProgress();
            getMoveCardActions();
        }

        @Test
        public void shouldReturnEmptyActionList() {
            assertThat(generatedActions, is(notNullValue()));
            assertThat(generatedActions.size(), equalTo(0));
        }
    }

    public class withOneReadyCardInAllWorkColumns {
        @Before
        public void initAndDoAction() {
            initialGameContainer.fillFirstWorkPhasesWithSomeReadyCards();
            initialGameContainer.fillLastWorkPhaseWithSomeReadyCards();
            getMoveCardActions();
        }

        @Test
        public void shouldReturnFiveActions() {
            assertThat(generatedActions.size(), equalTo(5));
        }

        @Test
        public void shouldMoveOneCardToDeployed() {
            shouldMoveOneCardTo(initialGameContainer.getDeployedPhase());
        }

        @Test
        public void shouldMoveOneCardToTest() {
            shouldMoveOneCardTo(initialGameContainer.getTestPhase());
        }

        @Test
        public void shouldMoveOneCardToDevelopment() {
            shouldMoveOneCardTo(initialGameContainer.getDevelopmentPhase());
        }

        private void shouldMoveOneCardTo(Phase phase) {
            String toColumnId = phase.getFirstColumn().getId();
            assertThat(generatedActions, hasItem(hasProperty("toColumnId", equalTo(toColumnId))));
        }
    }

    public class withReadyCardsAllColumnsFull {
        private List<Card> cardsInTesting;
        private String testColumnId;

        @Before
        public void initAndDoAction() {
            initialGameContainer.fillFirstWorkPhasesToFullWithReadyCards();
            initialGameContainer.fillLastWorkPhaseToFullWithReadyCards();
            cardsInTesting = initialGameContainer.getTestPhase().getFirstColumn().getCards();
            testColumnId = initialGameContainer.getTestPhase().getFirstColumn().getId();
            getMoveCardActions();
        }

        @Test
        public void shouldEmptyTestingFromItsInitialCards() {
            String deployedColumnId = initialGameContainer.getDeployedPhase().getFirstColumn().getId();
            cardsInTesting.forEach(cardInTesting -> {
                MoveCardAction moveCardAction = generatedActions.stream()
                        .filter(action -> action.getCardId().equals(cardInTesting.getId()))
                        .filter(action -> action.getFromColumnId().equals(testColumnId))
                        .findFirst().orElse(null);
                assertThat(moveCardAction, is(notNullValue()));
                assertThat(moveCardAction.getToColumnId(), equalTo(deployedColumnId));
            });
        }

        @Test
        public void shouldFillTestingWithCardsFromDevelopmentReady() {
            String developmentReadyColumn = initialGameContainer.getDevelopmentPhase().getSecondColumn().getId();
            Integer cardsFromDevelopmentReady = (int) generatedActions.stream()
                    .filter(action -> action.getToColumnId().equals(testColumnId))
                    .filter(action -> action.getFromColumnId().equals(developmentReadyColumn))
                    .count();

            Integer cardsFromDevReady = 2;
            Integer cardsFromDevInProgressThatWentThroughDevReady = 1;
            Integer allCardsFromDevReady = cardsFromDevReady + cardsFromDevInProgressThatWentThroughDevReady;
            assertThat(cardsFromDevelopmentReady, equalTo(allCardsFromDevReady));
        }
    }

    public class situationsIllustratingHowCardsMove {
        public class withCardsThatCantMoveToNextPhaseButCanMoveToNextColumn {
            @Before
            public void initAndDoAction() {
                initialGameContainer.fillDevelopmentWithSomeReadyCards();
                initialGameContainer.fillTestingToFullWithCardsInProgress();
                getMoveCardActions();
            }

            @Test
            public void shouldMoveCardsToDevelopmentSecondColumn() {
                String developmentSecondColumnId = initialGameContainer.getDevelopmentPhase().getSecondColumn().getId();
                assertThat(generatedActions, hasItem(
                        hasProperty("toColumnId", equalTo(developmentSecondColumnId))));
            }

            @Test
            public void shouldNotMoveAnyOtherCardsThanDevelopmentFirstColumnCards() {
                String developmentFirstColumnId = initialGameContainer.getDevelopmentPhase().getFirstColumn().getId();
                List<MoveCardAction> actionsFromOtherColumnsThanDevelopmentFirstColumn = generatedActions.stream()
                        .filter(action -> !action.getFromColumnId().equals(developmentFirstColumnId))
                        .collect(Collectors.toList());
                assertThat(actionsFromOtherColumnsThanDevelopmentFirstColumn.size(), equalTo(0));
            }

            @Test
            public void shouldNotMoveToAnyOtherPlacesThanToDevelopmentSecondColumn() {
                String developmentSecondColumnId = initialGameContainer.getDevelopmentPhase().getSecondColumn().getId();
                List<MoveCardAction> actionsToOtherColumnsThanDevelopmentSecondColumn = generatedActions.stream()
                        .filter(action -> !action.getToColumnId().equals(developmentSecondColumnId))
                        .collect(Collectors.toList());
                assertThat(actionsToOtherColumnsThanDevelopmentSecondColumn.size(), equalTo(0));
            }

        }

        //regression-test to found bug https://trello.com/c/9gKckxpC/83-bugi-wip-limitin-muutos-aiheuttaa-500-errorin-bakkarilla-joskus
        public class withCardsThatShouldNotBeAbleToMoveToNextPhase {
            @Before
            public void setTestWipLimitAndDoAction() {
                List<CardPhasePoint> pointsForDevFirstColumnCards = createCardPhasePointsForAnalysisDone();
                List<CardPhasePoint> pointsForDevSecondColumnAndTestPhaseCards = createCardPhasePointsForAnalysisAndDevDone();
                initialGameContainer.getDevelopmentPhase().getFirstColumn().setCards(Arrays.asList(
                    CardBuilder.aCard().withCardPhasePoints(pointsForDevFirstColumnCards).build(),
                    CardBuilder.aCard().withCardPhasePoints(pointsForDevFirstColumnCards).build(),
                    CardBuilder.aCard().withCardPhasePoints(pointsForDevFirstColumnCards).build()
                ));
                initialGameContainer.getDevelopmentPhase().getSecondColumn().setCards(Arrays.asList(
                    CardBuilder.aCard().withCardPhasePoints(pointsForDevSecondColumnAndTestPhaseCards).build()
                ));
                initialGameContainer.getTestPhase().getFirstColumn().setCards(Arrays.asList(
                    CardBuilder.aCard().withCardPhasePoints(pointsForDevSecondColumnAndTestPhaseCards).build(),
                    CardBuilder.aCard().withCardPhasePoints(pointsForDevSecondColumnAndTestPhaseCards).build(),
                    CardBuilder.aCard().withCardPhasePoints(pointsForDevSecondColumnAndTestPhaseCards).build()
                ));
                initialGameContainer.getTestPhase().setWipLimit(0);

                getMoveCardActions();
            }

            @Test
            public void shouldReturnEmptyActionList() {
                assertThat(generatedActions, is(notNullValue()));
                assertThat(generatedActions.size(), equalTo(0));
            }
        }

        public class withCardsThatCanMoveDirectlyToTheNextPhase {
            private List<String> distinctCardIdsThatHaveMoved;

            @Before
            public void initAndDoAction() {
                initialGameContainer.fillDevelopmentToFullWithReadyCards();
                moveDevelopmentCardsToFirstColumn();
                ensureAllCardsFitIntoTesting();
                getMoveCardActions();
                setDistinctCardIdsWithActions();
            }

            private void setDistinctCardIdsWithActions() {
                distinctCardIdsThatHaveMoved = generatedActions.stream()
                        .map(MoveCardAction::getCardId)
                        .distinct()
                        .collect(Collectors.toList());
            }

            private void moveDevelopmentCardsToFirstColumn() {
                List<Card> allDevelopmentCards = initialGameContainer.getDevelopmentPhase().getAllCards();
                initialGameContainer.getDevelopmentPhase().getFirstColumn().setCards(allDevelopmentCards);
                initialGameContainer.getDevelopmentPhase().getSecondColumn().setCards(new ArrayList<>());
            }

            private void ensureAllCardsFitIntoTesting() {
                Integer developmentWipLimit = initialGameContainer.getDevelopmentPhase().getWipLimit();
                initialGameContainer.getTestPhase().setWipLimit(developmentWipLimit);
            }

            @Test
            public void shouldMoveAllCardsFromDevelopment() {
                assertThat(distinctCardIdsThatHaveMoved.size(), equalTo(initialGameContainer.getDevelopmentPhase().getWipLimit()));
            }

            @Test
            public void shouldMoveCardsFromDevelopmentFirstColumnThroughSecondColumnToTesting() {
                distinctCardIdsThatHaveMoved.forEach(this::shouldHaveMovedFromDevelopmentFirstColumnThroughSecondColumnToTesting);
            }

            private void shouldHaveMovedFromDevelopmentFirstColumnThroughSecondColumnToTesting(String cardId) {
                shouldHaveTwoActions(cardId);
                shouldHaveActionFromDevelopmentFirstColumnToSecond(cardId);
                shouldHaveActionFromDevelopmentSecondColumnToTesting(cardId);
            }

            private void shouldHaveTwoActions(String cardId) {
                Integer amountOfActions = (int)generatedActions.stream()
                        .filter(action -> action.getCardId().equals(cardId))
                        .count();
                assertThat(amountOfActions, equalTo(2));
            }

            private void shouldHaveActionFromDevelopmentFirstColumnToSecond(String cardId) {
                String developmentFirstColumn = initialGameContainer.getDevelopmentPhase().getFirstColumn().getId();
                String developmentSecondColumn = initialGameContainer.getDevelopmentPhase().getSecondColumn().getId();
                Integer amountOfActions = (int)generatedActions.stream()
                        .filter(action -> action.getCardId().equals(cardId))
                        .filter(action -> action.getFromColumnId().equals(developmentFirstColumn))
                        .filter(action -> action.getToColumnId().equals(developmentSecondColumn))
                        .count();
                assertThat(amountOfActions, equalTo(1));
            }

            private void shouldHaveActionFromDevelopmentSecondColumnToTesting(String cardId) {
                String developmentSecondColumn = initialGameContainer.getDevelopmentPhase().getSecondColumn().getId();
                String testingFirstColumn = initialGameContainer.getTestPhase().getFirstColumn().getId();
                Integer amountOfActions = (int)generatedActions.stream()
                        .filter(action -> action.getCardId().equals(cardId))
                        .filter(action -> action.getFromColumnId().equals(developmentSecondColumn))
                        .filter(action -> action.getToColumnId().equals(testingFirstColumn))
                        .count();
                assertThat(amountOfActions, equalTo(1));
            }
        }
    }

    private void getMoveCardActions() {
        generatedActions = moveCardsAIService.getMoveCardsActions(initialGameContainer.getGame());
        toColumnIds = generatedActions.stream().map(MoveCardAction::getToColumnId).collect(Collectors.toList());
    }

    private List<CardPhasePoint> createCardPhasePointsForAnalysisDone() {
        return Arrays.asList(
                CardPhasePointBuilder.aCardPhasePoint().withPhaseId(ANALYSIS_PHASE_ID)
                        .withPointsDone(5)
                        .withTotalPoints(5)
                        .build(),
                CardPhasePointBuilder.aCardPhasePoint().withPhaseId(DEVELOPMENT_PHASE_ID)
                        .withPointsDone(0)
                        .withTotalPoints(5)
                        .build(),
                CardPhasePointBuilder.aCardPhasePoint().withPhaseId(TEST_PHASE_ID)
                        .withPointsDone(0)
                        .withTotalPoints(5)
                        .build()
        );
    }

    private List<CardPhasePoint> createCardPhasePointsForAnalysisAndDevDone() {
        return Arrays.asList(
                CardPhasePointBuilder.aCardPhasePoint().withPhaseId(ANALYSIS_PHASE_ID)
                        .withPointsDone(5)
                        .withTotalPoints(5)
                        .build(),
                CardPhasePointBuilder.aCardPhasePoint().withPhaseId(DEVELOPMENT_PHASE_ID)
                        .withPointsDone(5)
                        .withTotalPoints(5)
                        .build(),
                CardPhasePointBuilder.aCardPhasePoint().withPhaseId(TEST_PHASE_ID)
                        .withPointsDone(0)
                        .withTotalPoints(5)
                        .build()
        );
    }

}
