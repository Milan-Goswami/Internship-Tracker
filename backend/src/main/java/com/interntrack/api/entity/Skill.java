package com.interntrack.api.entity;

import com.interntrack.api.entity.enums.SkillCategory;
import jakarta.persistence.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(
    name = "skills",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_skill_canonical", columnNames = {"canonical_name"})
    }
)
public class Skill {

    private static final Map<String, String> ALIAS_MAP;

    static {
        Map<String, String> map = new HashMap<>();

        // C++ / Cpp -> cpp
        map.put("c++", "cpp");
        map.put("cpp", "cpp");

        // C# / Csharp -> csharp (prevents collision with C / C++)
        map.put("c#", "csharp");
        map.put("csharp", "csharp");

        // Node.js / NodeJS / Node -> nodejs
        map.put("node.js", "nodejs");
        map.put("nodejs", "nodejs");
        map.put("node", "nodejs");
        map.put("node js", "nodejs");

        // React.js / ReactJS / React -> react
        map.put("react.js", "react");
        map.put("reactjs", "react");
        map.put("react", "react");
        map.put("react js", "react");

        // .NET Core / ASP.NET / .NET -> dotnetcore
        map.put(".net core", "dotnetcore");
        map.put(".netcore", "dotnetcore");
        map.put("dotnet core", "dotnetcore");
        map.put("dotnetcore", "dotnetcore");
        map.put("asp.net", "dotnetcore");
        map.put("asp.net core", "dotnetcore");
        map.put("aspnet", "dotnetcore");
        map.put("aspnetcore", "dotnetcore");
        map.put(".net", "dotnetcore");
        map.put("dotnet", "dotnetcore");
        map.put("net", "dotnetcore");
        map.put("netcore", "dotnetcore");

        // Spring Boot / SpringBoot -> springboot
        map.put("spring boot", "springboot");
        map.put("springboot", "springboot");

        // PostgreSQL / Postgres -> postgresql
        map.put("postgresql", "postgresql");
        map.put("postgres", "postgresql");

        ALIAS_MAP = Collections.unmodifiableMap(map);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "canonical_name", nullable = false, length = 100)
    private String canonicalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private SkillCategory category;

    public Skill() {
    }

    public Skill(String name, SkillCategory category) {
        this.name = name;
        this.canonicalName = normalizeCanonicalName(name);
        this.category = category;
    }

    public static String normalizeCanonicalName(String rawName) {
        if (rawName == null) {
            return null;
        }
        String trimmed = rawName.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return "";
        }
        // Direct alias lookup (preserves symbols like +, #, ., etc.)
        String alias = ALIAS_MAP.get(trimmed);
        if (alias != null) {
            return alias;
        }
        // Collapsed whitespace lookup
        String collapsed = trimmed.replaceAll("\\s+", " ");
        alias = ALIAS_MAP.get(collapsed);
        if (alias != null) {
            return alias;
        }
        // Stripped alphanumeric fallback for aliases
        String stripped = trimmed.replaceAll("[^a-z0-9]", "");
        alias = ALIAS_MAP.get(stripped);
        if (alias != null) {
            return alias;
        }
        // Default canonicalization: lowercase alphanumeric
        return stripped;
    }

    @PrePersist
    protected void onCreate() {
        if (this.canonicalName == null && this.name != null) {
            this.canonicalName = normalizeCanonicalName(this.name);
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (this.canonicalName == null && name != null) {
            this.canonicalName = normalizeCanonicalName(name);
        }
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public SkillCategory getCategory() {
        return category;
    }

    public void setCategory(SkillCategory category) {
        this.category = category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Skill skill = (Skill) o;
        return id != null && Objects.equals(id, skill.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Skill{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", canonicalName='" + canonicalName + '\'' +
                ", category=" + category +
                '}';
    }
}