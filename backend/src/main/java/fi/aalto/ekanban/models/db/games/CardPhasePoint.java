package fi.aalto.ekanban.models.db.games;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document
public class CardPhasePoint {

    @Id
    private String id;

    @Field
    private String phaseId;

    @Field
    private Integer totalPoints;

    @Field
    private Integer pointsDone;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhaseId() {
        return phaseId;
    }

    public void setPhaseId(String phaseId) {
        this.phaseId = phaseId;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
        ensurePointsDoneNotMoreThanTotalPoints();
    }

    public Integer getPointsDone() {
        return pointsDone;
    }

    public void setPointsDone(Integer pointsDone) {
        this.pointsDone = pointsDone;
        ensurePointsDoneNotMoreThanTotalPoints();
    }

    public void increasePointsDoneBy(Integer points) {
        if (pointsDone == null) {
            pointsDone = points;
        }
        else {
            pointsDone += points;
        }
        ensurePointsDoneNotMoreThanTotalPoints();
    }

    public Integer pointsUntilDone() {
        return getTotalPoints() - getPointsDone();
    }

    @JsonIgnore
    public Boolean isReady() {
        return getTotalPoints().equals(getPointsDone());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (getClass() != o.getClass()) return false;
        if (!(o instanceof CardPhasePoint)) return false;

        CardPhasePoint that = (CardPhasePoint) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (phaseId != null ? !phaseId.equals(that.phaseId) : that.phaseId != null) return false;
        if (totalPoints != null ? !totalPoints.equals(that.totalPoints) : that.totalPoints != null) return false;
        return pointsDone != null ? pointsDone.equals(that.pointsDone) : that.pointsDone == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (phaseId != null ? phaseId.hashCode() : 0);
        result = 31 * result + (totalPoints != null ? totalPoints.hashCode() : 0);
        result = 31 * result + (pointsDone != null ? pointsDone.hashCode() : 0);
        return result;
    }

    private void ensurePointsDoneNotMoreThanTotalPoints() {
        if (pointsDone == null) {
            return;
        }
        pointsDone = Math.min(pointsDone, totalPoints);
    }
}
