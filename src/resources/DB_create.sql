CREATE SCHEMA iasion AUTHORIZATION orufeo ;

SET search_path TO 'iasion';

create table useraccount
(
  id serial NOT NULL,
  "timestamp" timestamp without time zone DEFAULT now(),
  metadata jsonb,
  data jsonb,
  CONSTRAINT useraccount_pk PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ON useraccount((metadata->>'guid')); 


create table wallet
(
  id serial NOT NULL,
  "timestamp" timestamp without time zone DEFAULT now(),
  metadata jsonb,
  data jsonb,
  CONSTRAINT wallet_pk PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ON wallet((metadata->>'guid')); 


create table wallet_history
(
  id serial NOT NULL,
  "timestamp" timestamp without time zone DEFAULT now(),
  metadata jsonb,
  data jsonb,
  CONSTRAINT wallet_history_pk PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ON wallet_history((metadata->>'guid')); 
CREATE INDEX wallet_history_expr_idx1 ON wallet_history(cast(data->>'walletGuid' AS text));

create table exchange
(
  id serial NOT NULL,
  "timestamp" timestamp without time zone DEFAULT now(),
  metadata jsonb,
  data jsonb,
  CONSTRAINT exchange_pk PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ON exchange((metadata->>'guid')); 

create table order
(
  id serial NOT NULL,
  "timestamp" timestamp without time zone DEFAULT now(),
  metadata jsonb,
  data jsonb,
  CONSTRAINT order_pk PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ON order((metadata->>'guid')); 

ALTER TABLE useraccount OWNER TO orufeo;
ALTER TABLE wallet OWNER TO orufeo;
ALTER TABLE wallet_history OWNER TO orufeo;
ALTER TABLE exchange OWNER TO orufeo;
ALTER TABLE order OWNER TO orufeo;
