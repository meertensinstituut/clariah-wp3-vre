import React from "react";
import {Redirect} from 'react-router-dom';
import queryString from 'query-string';
import {ControlLabel, FormControl, FormGroup, HelpBlock, Pagination} from 'react-bootstrap';
import ServiceSelector from "./service-selector";
import Switchboard from "../common/switchboard";

export default class Deploy extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            redirect: null,
            service: null,
            label: "Do you breathe?",
            description: "Breathing is very important",
            value: "",
            placeholder: "Hello? Is it me you're looking for?"
        };

        this.handleSelect = this.handleSelect.bind(this);

        this.state.urlParams = queryString.parse(this.props.location.search);

    }

    handleValidationFormBasicText() {
        const length = this.state.value.length;
        if (length > 10) return 'success';
        else if (length > 5) return 'warning';
        else if (length > 0) return 'error';
        return null;
    }

    handleBackClick() {
        this.setState({redirect: "/files"});
    }

    handleSelect(service) {
        this.setState({service: service});
        Switchboard.getParams(service.id).done((data) => {
            console.log(JSON.stringify(data, null, 2));
        });
    }

    getParams() {

    }


    render() {
        if (this.state.redirect !== null) return <Redirect to='/files'/>;

        return (
            <div>
                <div>
                    <Pagination>
                        <Pagination.Item onClick={() => this.handleBackClick()}>&lt; Back</Pagination.Item>
                        <Pagination.Item>Next step &gt;</Pagination.Item>
                    </Pagination>
                </div>
                <div>
                    <ServiceSelector
                        file={Number(this.state.urlParams.file)}
                        onSelect={this.handleSelect}
                    />
                </div>
                <div className="clearfix"/>
                <div>
                    <form>
                        <FormGroup
                            controlId="formBasicText"
                            validationState={this.handleValidationFormBasicText()}
                        >
                            <ControlLabel>{this.state.label}</ControlLabel>
                            <FormControl
                                type="text"
                                value={this.state.value}
                                placeholder={this.state.placeholder}
                                onChange={this.handleChange}
                            />
                            <FormControl.Feedback/>
                            <HelpBlock>{this.state.description}</HelpBlock>
                        </FormGroup>
                    </form>
                </div>
                <div>
                    <p>state:</p>
                    <pre>{JSON.stringify(this.state, null, 2)}</pre>
                </div>
                <div>
                    <p>props:</p>
                    <pre>{JSON.stringify(this.props, null, 2)}</pre>
                </div>
            </div>
        );
    }
}
