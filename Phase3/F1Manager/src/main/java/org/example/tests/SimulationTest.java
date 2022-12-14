package org.example.tests;

import org.example.business.Championship;
import org.example.business.Race;
import org.example.business.participants.Participant;
import org.example.business.systems.RaceSystem;
import org.example.business.users.Admin;
import org.example.data.*;
import org.example.exceptions.system.SystemException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.example.tests.AdminTest.createAdmin;

public class SimulationTest {
    private static final PlayerDAO udb = PlayerDAO.getInstance();
    private static final DriverDAO ddb = DriverDAO.getInstance();
    private static final RaceCarDAO rdb = RaceCarDAO.getInstance();


    @Test
    public void test() throws InterruptedException, SystemException {
        RaceSystem rs = new RaceSystem();
        Admin a = createAdmin(1).stream().map(n -> AdminDAO.getInstance().get(n)).reduce(null, (o, n) -> n);
        Championship c = new Championship(a);
        c = ChampionshipDAO.getInstance().put(c);
        Championship finalC = c;
        Race r = RaceTest.createRace(c.getId(), 1).stream()
                .map(x -> RaceDAO.getInstance(finalC.getId()).get(x))
                .limit(1)
                .reduce(null, (o, n) -> n);

        for (Participant p : c.getParticipants().values()) {
            rs.prepareForRace(c.getId(), r.getId(), p.getManager().getUsername());
        }

        while (true) {
            Race temp = rs.getRaceState(c.getId(), r.getId());

            List<Double> gaps = temp.getGaps();
            List<Participant> participants = temp.getResults();

            System.out.println("==============================");
            System.out.println("Lap: " + temp.getCurrentLap());
            System.out.println("Weather: " + temp.getWeatherConditions().toString());
            for (int i = 0; i < gaps.size(); i++) {
                System.out.print((i + 1) + ". ");
                System.out.print(participants.get(i).getDriver().getDriverName());
                System.out.print(" ");
                System.out.println(gaps.get(i));
            }
            System.out.println("==============================");
            Thread.sleep(1001);
        }
    }
}
