package pro.gravit.simplecabinet.web.model.shop;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pro.gravit.simplecabinet.web.model.user.Group;

@Setter
@Getter
@Entity
@Table(name = "group_products")
public class GroupProduct extends Product {
    private String name;
    private String server;
    private String world;
    private String context;
    @Column(name = "expire_days")
    private long expireDays;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_name")
    private Group group;
    private boolean local;
    private boolean stackable;

}
