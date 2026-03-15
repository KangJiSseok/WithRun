package kr.withrun.was.domain.user.repository.query;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.user.entity.QUser;
import kr.withrun.was.domain.user.entity.QUserCalendar;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarSummaryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserCalendarCustomRepositoryImpl implements UserCalendarCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public UserCalendarSummaryRow findSummaryRow(Long userId) {
        QUser user = QUser.user;
        QUserCalendar userCalendar = QUserCalendar.userCalendar;
        NumberExpression<Integer> runCountExpression = userCalendar.courseRunCount
                .add(userCalendar.freeRunCount)
                .add(userCalendar.ghostRunCount);

        UserCalendarSummaryRow summaryRow = queryFactory
                .select(Projections.constructor(
                        UserCalendarSummaryRow.class,
                        runCountExpression.sum().coalesce(0),
                        userCalendar.totalDistanceM.sum().coalesce(0),
                        userCalendar.courseRunCount.sum().coalesce(0),
                        userCalendar.freeRunCount.sum().coalesce(0),
                        userCalendar.ghostRunCount.sum().coalesce(0)
                ))
                .from(userCalendar)
                .join(userCalendar.user, user)
                .where(
                        user.id.eq(userId),
                        user.deletedAt.isNull(),
                        userCalendar.deletedAt.isNull()
                )
                .fetchOne();

        if (summaryRow == null) {
            return new UserCalendarSummaryRow(0, 0, 0, 0, 0);
        }

        return summaryRow;
    }

    @Override
    public List<LocalDate> findRunningDates(Long userId) {
        QUser user = QUser.user;
        QUserCalendar userCalendar = QUserCalendar.userCalendar;
        NumberExpression<Integer> runCountExpression = getIntegerNumberExpression(userCalendar);

        return queryFactory
                .select(userCalendar.calendarDate)
                .from(userCalendar)
                .join(userCalendar.user, user)
                .where(
                        user.id.eq(userId),
                        user.deletedAt.isNull(),
                        userCalendar.deletedAt.isNull(),
                        runCountExpression.gt(0)
                )
                .orderBy(userCalendar.calendarDate.desc())
                .fetch();
    }

    private static NumberExpression<Integer> getIntegerNumberExpression(QUserCalendar userCalendar) {
        NumberExpression<Integer> runCountExpression =
                userCalendar.courseRunCount
                        .add(userCalendar.freeRunCount)
                        .add(userCalendar.ghostRunCount);
        return runCountExpression;
    }
}
