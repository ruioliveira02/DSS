package org.example.data;


import org.example.business.Race;
import org.example.business.Weather;
import org.example.business.participants.Participant;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class RaceDAO implements Map<Integer, Race> {

    private static Map<Integer, RaceDAO> singletons = new HashMap<>();
    private final Map<Integer, Race> runningRaces = new HashMap<>();
    private int championship;

    private RaceDAO(int championship) {
        this.championship = championship;
        try (Connection conn = DatabaseData.getConnection();
             Statement stm = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS races (" +
                    "Id INT AUTO_INCREMENT PRIMARY KEY," +
                    "Championship INT NOT NULL," +
                    "WeatherVariability DECIMAL(11,10) NOT NULL," +
                    "Circuit VARCHAR(255) NOT NULL," +
                    "Finished BOOLEAN NOT NULL," +
                    "FOREIGN KEY (Championship) REFERENCES championships(Id) ON DELETE CASCADE ON UPDATE CASCADE" +
                    ");";
            stm.executeUpdate(sql);
            sql = "CREATE TABLE IF NOT EXISTS raceResults (" +
                    "Id INT ," +
                    "Position INT NOT NULL," +
                    "Participant VARCHAR(255) NOT NULL," +
                    "PRIMARY KEY (Id,Participant)," +
                    "FOREIGN KEY (Id) REFERENCES races(Id) ON DELETE CASCADE ON UPDATE CASCADE," +
                    "FOREIGN KEY (Participant) REFERENCES participants(Player) ON DELETE CASCADE ON UPDATE CASCADE" +
                    ");";
            stm.executeUpdate(sql);
            sql = "CREATE TABLE IF NOT EXISTS raceReady (" +
                    "Id INT ," +
                    "Ready BOOLEAN NOT NULL," +
                    "Participant VARCHAR(255) NOT NULL," +
                    "PRIMARY KEY (Id,Participant)," +
                    "FOREIGN KEY (Id) REFERENCES races(Id) ON DELETE CASCADE ON UPDATE CASCADE," +
                    "FOREIGN KEY (Participant) REFERENCES participants(Player) ON DELETE CASCADE ON UPDATE CASCADE" +
                    ");";
            stm.executeUpdate(sql);
        } catch (SQLException e) {
            // Erro a criar tabela...
            e.printStackTrace();
            throw new NullPointerException(e.getMessage());
        }
    }

    /**
     * implementation of the singleton pattern
     *
     * @return returns the only instance of the class
     */
    public static RaceDAO getInstance(int championship) {
        if (!RaceDAO.singletons.containsKey(championship)) {
            RaceDAO.singletons.put(championship, new RaceDAO(championship));
        }
        return RaceDAO.singletons.get(championship);
    }

    public void addRunningRace(Race r) {
        runningRaces.put(r.getId(), r);
    }

    public void removeRunningRace(Race r) {
        runningRaces.remove(r.getId());
    }

    private Race getRunningRace(Integer i) {
        return runningRaces.get(i);
    }

    /**
     * @return number of users in the system
     */
    @Override
    public int size() {
        int i = 0;
        try (Connection conn = DatabaseData.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM races WHERE Championship=?;");) {
            ps.setInt(1, championship);
            try (ResultSet rs = ps.executeQuery();) {
                if (rs.next())
                    i = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new NullPointerException(e.getMessage());
        }
        return i;
    }

    /**
     * Method that verifies the existence of users
     *
     * @return true if the number of users is 0
     */
    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    /**
     * method which checks if a user with a given username exists in the database
     *
     * @param key username
     * @return true if the user exists
     * @throws NullPointerException //TODO MUDAR ISTO
     */
    @Override
    public boolean containsKey(Object key) {
        if (!(Integer.class.isInstance(key)))
            return false;
        boolean r = false;
        try (Connection conn = DatabaseData.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT Id FROM races WHERE Championship=? AND Id= ?;");) {
            ps.setInt(1, championship);
            ps.setInt(2, (Integer) key);
            try (ResultSet rs = ps.executeQuery();) {
                if (rs.next())
                    r = true;
            }
        } catch (SQLException e) {
            // Database error!
            e.printStackTrace();
            throw new NullPointerException(e.getMessage());
        }
        return r;
    }


    public List<Participant> participants(Integer key) {
        List<Participant> res = new ArrayList<>();
        try (Connection conn = DatabaseData.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT Participant FROM raceResults WHERE Id= ? ORDER BY Position ASC;");) {
            ps.setInt(1, key);
            try (ResultSet rs = ps.executeQuery();) {
                while (rs.next()) {
                    res.add(ParticipantDAO.getInstance(championship).get(rs.getString("Participant")));
                }
            }
        } catch (SQLException e) {
            // Database error!
            e.printStackTrace();
            throw new NullPointerException(e.getMessage());
        }
        return res;
    }

    public Map<Participant, Boolean> ready(Integer key) {
        Map<Participant, Boolean> res = new HashMap<>();
        try (Connection conn = DatabaseData.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT Participant,Ready FROM raceReady WHERE Id= ?;");) {
            ps.setInt(1, key);
            try (ResultSet rs = ps.executeQuery();) {
                while (rs.next()) {
                    res.put(ParticipantDAO.getInstance(championship).get(rs.getString("Participant")), rs.getBoolean("Ready"));
                }
            }
        } catch (SQLException e) {
            // Database error!
            e.printStackTrace();
            throw new NullPointerException(e.getMessage());
        }
        return res;
    }


    @Override
    public Race get(Object key) {
        if (!(Integer.class.isInstance(key)))
            return null;
        Race r = runningRaces.get(key);
        if (r != null) return r;
        try (Connection conn = DatabaseData.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT Id,Championship,WeatherVariability,Circuit,Finished FROM races WHERE Championship=? AND Id= ?;");) {
            ps.setInt(1, championship);
            ps.setInt(2, (Integer) key);
            try (ResultSet rs = ps.executeQuery();) {
                if (rs.next()) {
                    boolean b = rs.getBoolean("Finished");
                    Map<Participant, Boolean> readys = ready((Integer) key);
                    return new Race(
                            rs.getInt("Id"),
                            rs.getInt("Championship"),
                            b,
                            new Weather(rs.getDouble("WeatherVariability")),
                            CircuitDAO.getInstance().get(rs.getString("Circuit")),
                            b ? participants((Integer) key) :
                                    readys.keySet().stream().collect(Collectors.toCollection(ArrayList::new)),
                            readys
                    );
                }
            }
        } catch (SQLException e) {
            // Database error!
            e.printStackTrace();
            throw new NullPointerException(e.getMessage());
        }
        return null;
    }

    @Override
    public boolean containsValue(Object value) {
        if (!(value instanceof Race)) return false;
        Race p = (Race) value;
        return p.equals(get(p.getId()));
    }


    @Override
    public Race put(Integer key, Race race) {
        if (race.getChampionshipId() != championship) return null;
        String sql = "";
        if (key == null)
            sql = "INSERT INTO races (Championship,WeatherVariability,Circuit,Finished) VALUES (?,?,?,?);";
        else
            sql = "INSERT INTO races (Id,Championship,WeatherVariability,Circuit,Finished) VALUES (?,?,?,?,?);";

        try (Connection conn = DatabaseData.getConnection();) {
            conn.setAutoCommit(false);
            try (
                    PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ) {
                int n = 1;
                if (key != null) {
                    ps.setInt(n, race.getId());
                    n++;
                }
                ps.setInt(n, race.getChampionshipId());
                n++;
                ps.setDouble(n, race.getWeatherConditions().getVariability());
                n++;
                ps.setString(n, race.getTrack().getName());
                n++;
                ps.setBoolean(n, race.hasFinished());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys();) {
                    if (rs.next())
                        race.setId(rs.getInt(1));
                }
            } catch (SQLException e) {
                return null;
            }
            try (
                    PreparedStatement ps = conn.prepareStatement("INSERT INTO raceResults (Id,Position,Participant) VALUES (?,?,?) ");
            ) {
                List<Participant> t = race.getResults();
                for (int i = 0; i < t.size(); i++) {
                    ps.setInt(1, race.getId());
                    ps.setInt(2, i);
                    ps.setString(3, t.get(i).getManager().getUsername());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                return null;
            }
            try (
                    PreparedStatement ps = conn.prepareStatement("INSERT INTO raceReady (Id,Ready,Participant) VALUES (?,?,?) ");
            ) {
                for (Entry<Participant, Boolean> e : race.getReady().entrySet()) {
                    ps.setInt(1, race.getId());
                    ps.setBoolean(2, e.getValue());
                    ps.setString(3, e.getKey().getManager().getUsername());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                return null;
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            return null;
        }
        return race;
    }

    public Race put(Race race) {
        return this.put(race.getId(), race);
    }


    public Race update(Race race) {
        if (race.getId() == null || race.getChampionshipId() != championship)
            return null;
        try (
                Connection conn = DatabaseData.getConnection();) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE races SET Championship=?,WeatherVariability=?,Circuit=?,Finished=? WHERE Id=?;");) {
                ps.setInt(1, race.getChampionshipId());
                ps.setDouble(2, race.getWeatherConditions().getVariability());
                ps.setString(3, race.getTrack().getName());
                ps.setBoolean(4, race.hasFinished());
                ps.setInt(5, race.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM raceResults WHERE ID=?;");) {
                ps.setInt(1, race.getId());
                ps.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO raceResults (Id,Position,Participant) VALUES (?,?,?) ON DUPLICATE KEY UPDATE Position=?;");) {
                List<Participant> t = race.getResults();
                for (int i = 0; i < t.size(); i++) {
                    ps.setInt(1, race.getId());
                    ps.setInt(2, i);
                    ps.setString(3, t.get(i).getManager().getUsername());
                    ps.setInt(4, i);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO raceReady (Id,Ready,Participant) VALUES (?,?,?) ON DUPLICATE KEY UPDATE Ready=?;");) {
                for (Entry<Participant, Boolean> e : race.getReady().entrySet()) {
                    ps.setInt(1, race.getId());
                    ps.setBoolean(2, e.getValue());
                    ps.setString(3, e.getKey().getManager().getUsername());
                    ps.setBoolean(4, e.getValue());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return race;
    }


    @Override
    public Race remove(Object key) {
        Race value = this.get(key);
        if (value == null || value.getChampionshipId() != championship)
            return null;
        try (Connection conn = DatabaseData.getConnection();) {
            conn.setAutoCommit(false);
            String[] tables = {"races", "raceResults", "raceReady"};
            for (String table : tables) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE Id = ?;");) {
                    ps.setInt(1, (Integer) key);
                    ps.executeUpdate();
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
            return value;
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends Race> m) {
        try (Connection conn = DatabaseData.getConnection();) {
            conn.setAutoCommit(false);
            for (Entry<? extends Integer, ? extends Race> en : m.entrySet()) {
                Integer key = en.getKey();
                Race race = en.getValue();
                String sql = "";
                if (key == null)
                    sql = "INSERT INTO races (Championship,WeatherVariability,Circuit,Finished) VALUES (?,?,?,?);";
                else
                    sql = "INSERT INTO races (Id,Championship,WeatherVariability,Circuit,Finished) VALUES (?,?,?,?,?);";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);) {
                    int n = 1;
                    if (key != null) {
                        ps.setInt(n, race.getId());
                        n++;
                    }
                    ps.setInt(n, race.getChampionshipId());
                    n++;
                    ps.setDouble(n, race.getWeatherConditions().getVariability());
                    n++;
                    ps.setString(n, race.getTrack().getName());
                    n++;
                    ps.setBoolean(n, race.hasFinished());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys();) {
                        if (rs.next())
                            race.setId(rs.getInt(1));
                    }
                } catch (SQLException e) {
                    return;
                }
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO raceResults (Id,Position,Participant) VALUES (?,?,?) ");) {
                    List<Participant> t = race.getResults();
                    for (int i = 0; i < t.size(); i++) {
                        ps.setInt(1, race.getId());
                        ps.setInt(2, i);
                        ps.setString(3, t.get(i).getManager().getUsername());
                        ps.executeUpdate();
                    }
                } catch (SQLException e) {
                    return;
                }
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO raceReady (Id,Ready,Participant) VALUES (?,?,?) ");) {
                    for (Entry<Participant, Boolean> e : race.getReady().entrySet()) {
                        ps.setInt(1, race.getId());
                        ps.setBoolean(2, e.getValue());
                        ps.setString(3, e.getKey().getManager().getUsername());
                        ps.executeUpdate();
                    }
                } catch (SQLException e) {
                    return;
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            return;
        }
    }


    @Override
    public void clear() {
        Set<Integer> ids = keySet();
        try (Connection conn = DatabaseData.getConnection();) {
            conn.setAutoCommit(false);
            String[] tables = {"races", "raceResults", "raceReady"};
            for (Integer id : ids) {
                for (String table : tables) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE Id = ?;");) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            return;
        }
    }


    @Override
    public Set<Integer> keySet() {
        Set<Integer> r = new HashSet<Integer>();
        try (Connection conn = DatabaseData.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT Id FROM races WHERE Championship=?;");) {
            ps.setInt(1, championship);
            try (ResultSet rs = ps.executeQuery();) {
                while (rs.next())
                    r.add(rs.getInt("Id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @Override
    public Collection<Race> values() {
        Collection<Race> r = new HashSet<Race>();
        try (Connection conn = DatabaseData.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT Id,Championship,WeatherVariability,Circuit,Finished FROM races WHERE Championship=?;");) {
            ps.setInt(1, championship);
            try (ResultSet rs = ps.executeQuery();) {
                while (rs.next()) {
                    int id = rs.getInt("Id");
                    boolean b = rs.getBoolean("Finished");
                    r.add(new Race(
                            id,
                            rs.getInt("Championship"),
                            b,
                            new Weather(rs.getDouble("WeatherVariability")),
                            CircuitDAO.getInstance().get(rs.getString("Circuit")),
                            b ? participants(id) : new ArrayList<Participant>(),
                            ready(id)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @Override
    public Set<Entry<Integer, Race>> entrySet() {
        return values().stream().collect(
                Collectors.toMap(Race::getId, x -> x)).entrySet();
    }
}