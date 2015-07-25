--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

--
-- Data for Name: region; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY region (region_id, region_name, region_description, region_thumbnail, super_region_id) FROM stdin;
\.


--
-- Data for Name: user; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY "user" ("userID", "firstName", "lastName", "fullName", email, "avatarURL") FROM stdin;
b2bb0612-db1e-46c0-b74e-7bebdd0fa0b7	Joao Rafael	Moraes Nicola	Joao Rafael Moraes Nicola	joaoraf@gmail.com	http://www.gravatar.com/avatar/74ae552dfe1d8c5ef778d3fa0977afc2?d=404
\.


--
-- Data for Name: trip; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY trip (trip_id, trip_name, trip_is_public, user_id, region_id) FROM stdin;
\.


--
-- Data for Name: trip_day; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY trip_day (trip_id, day_number, day_label) FROM stdin;
\.


--
-- Data for Name: activity; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY activity (trip_id, day_number, activity_order, length_hours) FROM stdin;
\.


--
-- Data for Name: city; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY city (city_id, city_name, city_description, region_id) FROM stdin;
\.


--
-- Data for Name: logininfo; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY logininfo (id, "providerID", "providerKey") FROM stdin;
1	credentials	joaoraf@gmail.com
\.


--
-- Name: logininfo_id_seq; Type: SEQUENCE SET; Schema: public; Owner: tripspace
--

SELECT pg_catalog.setval('logininfo_id_seq', 1, true);


--
-- Data for Name: oauth1info; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY oauth1info (id, token, secret, "loginInfoId") FROM stdin;
\.


--
-- Name: oauth1info_id_seq; Type: SEQUENCE SET; Schema: public; Owner: tripspace
--

SELECT pg_catalog.setval('oauth1info_id_seq', 1, false);


--
-- Data for Name: oauth2info; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY oauth2info (id, accesstoken, tokentype, expiresin, refreshtoken, logininfoid) FROM stdin;
\.


--
-- Name: oauth2info_id_seq; Type: SEQUENCE SET; Schema: public; Owner: tripspace
--

SELECT pg_catalog.setval('oauth2info_id_seq', 1, false);


--
-- Data for Name: passwordinfo; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY passwordinfo (hasher, password, salt, "loginInfoId") FROM stdin;
bcrypt	$2a$10$hWctRpJh97Yg/b3lbt328eh0/V99QcLj3Iyq/zq47bTYLX0kf.J1W	\N	1
\.


--
-- Data for Name: poi; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY poi (poi_id, poi_name, poi_description, city_id) FROM stdin;
\.


--
-- Data for Name: transport_activity; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY transport_activity (trip_id, day_number, activity_order, from_city_id, to_city_id, transport_modality_id, transport_description) FROM stdin;
\.


--
-- Data for Name: transport_modality; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY transport_modality (transport_modality_id, transport_modality_name) FROM stdin;
\.


--
-- Data for Name: userlogininfo; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY userlogininfo ("userID", "loginInfoId") FROM stdin;
b2bb0612-db1e-46c0-b74e-7bebdd0fa0b7	1
\.


--
-- Data for Name: visit_activity; Type: TABLE DATA; Schema: public; Owner: tripspace
--

COPY visit_activity (trip_id, day_number, activity_order, visit_poi_id, visit_description) FROM stdin;
\.


--
-- PostgreSQL database dump complete
--

