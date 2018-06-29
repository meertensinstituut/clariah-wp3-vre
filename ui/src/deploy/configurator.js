import React from "react";
import {ControlLabel, FormControl, FormGroup, HelpBlock} from 'react-bootstrap';
import Switchboard from "../common/switchboard";

export default class Configurator extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            label: "Do you breathe?",
            description: "Breathing is very important",
            value: "",
            placeholder: "Hello? Is it me you're looking for?"
        };
        if (this.props.service !== undefined) {
            this.getServiceParams(this.props.service);
        }
    }

    getServiceParams(service) {
        Switchboard.getParams(service).then((data) => {
            this.setState({
                serviceParams: data
            });
        });
    }

    handleValidationFormBasicText() {
        const length = this.state.value.length;
        if (length > 10) return 'success';
        if (length > 5) return 'warning';
        if (length > 0) return 'error';
        return null;
    }

    render() {
        return (
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