package com.bank.transactionapi.models;

import com.bank.transactionapi.enums.NetworkProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("AIRTIME")
@Getter
@Setter
@NoArgsConstructor
public class Airtime extends Transaction {

    @Enumerated(EnumType.STRING)
    private NetworkProvider networkProvider;

    private String phoneNumber;
}