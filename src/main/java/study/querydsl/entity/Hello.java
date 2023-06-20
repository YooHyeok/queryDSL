package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter
public class Hello {

    @Id @GeneratedValue
    private Long id;
}
